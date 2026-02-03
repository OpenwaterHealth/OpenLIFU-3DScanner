package health.openwater.openlifu3dscanner.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import health.openwater.openlifu3dscanner.core.ImageUploader
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.PhotoscanService
import health.openwater.openlifu3dscanner.network.api.WebsocketService
import health.openwater.openlifu3dscanner.network.dto.CreatePhotocollectionRequest
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.Photoscan
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.network.dto.StartPhotoscanRequest
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.network.safeCall
import health.openwater.openlifu3dscanner.service.ReconstructionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconstructionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authService: AuthService,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val websocketService: WebsocketService,
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Current photocollection being uploaded
    private var currentPhotocollection: Photocollection? = null

    // Signals when photocollection is created and ready for upload
    private val _photocollectionReady = MutableStateFlow(false)
    val photocollectionReady: StateFlow<Boolean> = _photocollectionReady.asStateFlow()

    // Upload progress
    private val _imageUploadProgress = MutableStateFlow<ImageUploadProgress?>(null)
    val imageUploadProgress: StateFlow<ImageUploadProgress?> = _imageUploadProgress.asStateFlow()

    // Reconstruction progress
    private val _reconstructionProgress = MutableStateFlow<ReconstructionProgress?>(null)
    val reconstructionProgress: StateFlow<ReconstructionProgress?> =
        _reconstructionProgress.asStateFlow()

    // Overall upload state
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    // Current photoscan ID (after reconstruction starts)
    private var _currentPhotoscanId: Long? = null
    val currentPhotoscanId: Long? get() = _currentPhotoscanId

    // Image uploader instance
    private var imageUploader: ImageUploader? = null

    // Polling job for reconstruction progress (fallback when WebSocket fails in background)
    private var pollingJob: Job? = null

    // Current collection reference number
    var currentReferenceNumber: String? = null
        private set

    var totalImageCount: String? = null

    // Flag to indicate the scan is complete (all buckets captured or user pressed proceed)
    private var scanComplete = false

    init {
        observeProgress()
    }

    private fun observeProgress() {
        // Observe upload progress and update state accordingly
        scope.launch {
            _imageUploadProgress.collect { progress ->
                if (progress == null) return@collect

                Log.d(
                    TAG,
                    "Progress update: ${progress.uploadedImages}/${progress.totalImages}, failed=${progress.failed}"
                )

                // Transition to Uploading state when progress starts
                if (_uploadState.value == UploadState.Idle && progress.totalImages > 0) {
                    _uploadState.value = UploadState.Uploading
                }

                when {
                    progress.failed -> {
                        _uploadState.value = UploadState.Error("Upload failed")
                        ReconstructionService.stop(context)
                    }

                    progress.uploadedImages == progress.totalImages && progress.totalImages > 0 -> {
                        if (_uploadState.value == UploadState.Uploading && scanComplete) {
                            Log.d(
                                TAG,
                                "All images uploaded and scan complete! Starting reconstruction automatically..."
                            )
                            _uploadState.value = UploadState.UploadComplete
                            // Automatically start reconstruction when upload completes
                            // This ensures it works even when app is in background
                            startReconstructionAutomatically()
                        } else if (!scanComplete) {
                            Log.d(TAG, "All current images uploaded, waiting for scan to complete...")
                        }
                    }
                }
            }
        }

        // Observe reconstruction progress
        scope.launch {
            _reconstructionProgress.collect { progress ->
                if (progress == null) return@collect

                when (progress.status) {
                    PhotoscanStatus.STARTED, PhotoscanStatus.RUNNING -> {
                        if (_uploadState.value != UploadState.Reconstructing) {
                            _uploadState.value = UploadState.Reconstructing
                        }
                    }

                    PhotoscanStatus.FINISHED -> {
                        _currentPhotoscanId?.let { stopReconstructionProgressListener(it) }
                        ReconstructionService.stop(context)
                    }

                    PhotoscanStatus.FAILED -> {
                        _uploadState.value = UploadState.Error(
                            progress.message ?: "Reconstruction failed"
                        )
                        _currentPhotoscanId?.let { stopReconstructionProgressListener(it) }
                        ReconstructionService.stop(context)
                    }

                    PhotoscanStatus.STOPPED, null -> {}
                }
            }
        }
    }

    fun isLoggedInAndOnline(): Boolean {
        return runBlocking {
            authService.isSignedIn() && userRepository.isCloudAvailable()
        }
    }

    fun getCurrentPhotocollection(): Photocollection? = currentPhotocollection

    /**
     * Create a new photocollection for uploading images.
     * This starts the foreground service and prepares for upload.
     */
    fun createPhotocollection(name: String, autoUpload: Boolean) {
        Log.d(TAG, "Creating photocollection: $name, autoUpload: $autoUpload")

        this.currentReferenceNumber = name
        _uploadState.value = UploadState.Idle
        _photocollectionReady.value = false
        _imageUploadProgress.value = null
        scanComplete = false

        val uid = authService.getCurrentUser()?.uid
        if (uid == null) {
            Log.e(TAG, "User not logged in")
            _uploadState.value = UploadState.Error("User not logged in")
            return
        }

        // Start foreground service
        ReconstructionService.start(context)

        scope.launch {
            val result = safeCall {
                photocollectionService.createPhotocollection(
                    CreatePhotocollectionRequest(
                        accountId = uid,
                        name = name
                    )
                )
            }

            when (result) {
                is Result.Success -> {
                    currentPhotocollection = result.body
                    val collectionId = result.body.id

                    Log.d(TAG, "Photocollection created with ID: $collectionId")

                    // Create ImageUploader
                    imageUploader = ImageUploader(
                        context = context,
                        photocollectionId = collectionId,
                        imagesDir = getImagesDir(name),
                        progressFlow = _imageUploadProgress,
                        photocollectionService = photocollectionService,
                        scope = scope
                    )

                    _photocollectionReady.value = true

                    // Start uploading if auto-upload mode
                    if (autoUpload) {
                        Log.d(TAG, "Starting auto-upload")
                        imageUploader?.start(autoUpload = true, resetProgress = true)
                    }
                }

                is Result.NetworkError -> {
                    Log.e(TAG, "Network error creating photocollection: ${result.message}")
                    _uploadState.value = UploadState.Error("Network error: ${result.message}")
                    ReconstructionService.stop(context)
                }

                is Result.AuthError -> {
                    Log.e(TAG, "Auth error creating photocollection")
                    _uploadState.value = UploadState.Error("Authentication error")
                    ReconstructionService.stop(context)
                }

                is Result.ServerError -> {
                    Log.e(
                        TAG,
                        "Server error creating photocollection: ${result.code} ${result.message}"
                    )
                    _uploadState.value = UploadState.Error("Server error: ${result.message}")
                    ReconstructionService.stop(context)
                }

                is Result.UnexpectedError -> {
                    Log.e(TAG, "Unexpected error creating photocollection: ${result.message}")
                    _uploadState.value = UploadState.Error("Error: ${result.message}")
                    ReconstructionService.stop(context)
                }
            }
        }
    }

    /**
     * Notify that a new image has been captured.
     * Used in auto-upload mode to trigger upload of new images.
     */
    fun onImageCaptured() {
        imageUploader?.onImageCaptured()
    }

    /**
     * Mark the scan as complete (all buckets captured or user pressed proceed).
     * This allows reconstruction to start automatically once all images are uploaded.
     */
    fun onScanComplete() {
        Log.d(TAG, "Scan marked as complete")
        scanComplete = true
        // Re-evaluate progress to trigger reconstruction if all images are already uploaded
        _imageUploadProgress.value?.let { progress ->
            if (progress.uploadedImages == progress.totalImages && progress.totalImages > 0) {
                if (_uploadState.value == UploadState.Uploading) {
                    Log.d(TAG, "All images already uploaded, starting reconstruction now")
                    _uploadState.value = UploadState.UploadComplete
                    startReconstructionAutomatically()
                }
            }
        }
    }

    /**
     * Upload all remaining photos that haven't been uploaded yet.
     * Used for manual upload mode.
     */
    fun uploadRemainingPhotos() {
        Log.d(TAG, "uploadRemainingPhotos called")

        val uploader = imageUploader
        if (uploader == null) {
            Log.e(TAG, "ImageUploader is null - cannot upload")
            return
        }

        if (uploader.isRunning) {
            Log.d(TAG, "Uploader already running")
            return
        }

        if (!ReconstructionService.isRunning.value) {
            ReconstructionService.start(context)
        }

        // Start uploading (don't reset progress - continue from where we left off)
        uploader.start(autoUpload = false, resetProgress = false)
    }

    fun stopImageUploader() {
        imageUploader?.stop()
    }

    fun setStartingReconstruction() {
        _uploadState.value = UploadState.StartingReconstruction
    }

    /**
     * Automatically start reconstruction after upload completes.
     * Called from the progress observer, works even when app is in background.
     */
    private fun startReconstructionAutomatically() {
        scope.launch {
            setStartingReconstruction()
            startReconstructionFlow()
        }
    }

    suspend fun startReconstructionFlow(): Long? {
        // Prevent double-starting reconstruction
        if (_uploadState.value == UploadState.Reconstructing) {
            Log.d(TAG, "Reconstruction already in progress, skipping")
            return _currentPhotoscanId
        }

        Log.d(TAG, "Starting reconstruction flow")

        val photoscanId = startReconstruction()
        if (photoscanId != null) {
            _currentPhotoscanId = photoscanId
            startReconstructionProgressListener(photoscanId)
            _uploadState.value = UploadState.Reconstructing
        } else {
            _uploadState.value = UploadState.Error("Failed to start reconstruction")
            ReconstructionService.stop(context)
        }
        return photoscanId
    }

    private suspend fun startReconstruction(): Long? {
        val collectionId = currentPhotocollection?.id
        if (collectionId == null) {
            Log.e(TAG, "No current photocollection")
            return null
        }

        val result = safeCall {
            photocollectionService.startPhotoscan(collectionId, StartPhotoscanRequest())
        }

        return when (result) {
            is Result.Success -> {
                Log.d(TAG, "Reconstruction started with photoscan ID: ${result.body.photoscanId}")
                result.body.photoscanId
            }

            else -> {
                Log.e(TAG, "Failed to start reconstruction: $result")
                null
            }
        }
    }

    suspend fun startReconstructionProgressListener(photoscanId: Long): Photoscan? {
        val photoscan = getPhotoscan(photoscanId) ?: return null
        _reconstructionProgress.emit(
            ReconstructionProgress(
                photoscan.progress, photoscan.message, photoscan.status
            )
        )
        websocketService.connect(photoscanId, _reconstructionProgress)

        // Start polling as fallback (WebSocket may not work reliably in background)
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                try {
                    val updated = getPhotoscan(photoscanId)
                    if (updated != null) {
                        _reconstructionProgress.emit(
                            ReconstructionProgress(
                                updated.progress, updated.message, updated.status
                            )
                        )
                        // Stop polling if reconstruction is complete
                        if (updated.status == PhotoscanStatus.FINISHED ||
                            updated.status == PhotoscanStatus.FAILED ||
                            updated.status == PhotoscanStatus.STOPPED
                        ) {
                            Log.d(TAG, "Reconstruction finished, stopping polling")
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                }
            }
        }
        return photoscan
    }

    fun stopReconstructionProgressListener(photoscanId: Long) {
        pollingJob?.cancel()
        pollingJob = null
        websocketService.disconnect(photoscanId)
    }

    private suspend fun getPhotoscan(id: Long): Photoscan? {
        val result = safeCall { photoscanService.getPhotoscan(id) }
        return when (result) {
            is Result.Success -> result.body
            else -> {
                Log.e(TAG, "Failed to get photoscan: $result")
                null
            }
        }
    }

    /**
     * Reset the upload repository state.
     * @param removeLocalCollection If true, deletes the local files and server collection.
     */
    fun reset(removeLocalCollection: Boolean) {
        Log.d(TAG, "Resetting upload repository, removeLocalCollection: $removeLocalCollection")

        _currentPhotoscanId?.let { stopReconstructionProgressListener(it) }
        _currentPhotoscanId = null

        if (removeLocalCollection) {
            resetCurrentPhotocollection()
        } else {
            imageUploader?.stop()
            imageUploader = null
            currentPhotocollection = null
            _photocollectionReady.value = false
        }

        _uploadState.value = UploadState.Idle
        _imageUploadProgress.value = null
        _reconstructionProgress.value = null
        scanComplete = false
        ReconstructionService.stop(context)
    }

    fun resetCurrentPhotocollection() {
        deletePhotocollection()
        currentReferenceNumber?.let {
            val dir = getImagesDir(it)
            if (dir.exists()) {
                Log.d(TAG, "Deleting directory: $dir")
                dir.deleteRecursively()
            }
        }

        imageUploader?.stop()
        imageUploader = null
        currentReferenceNumber = null
        currentPhotocollection = null
        totalImageCount = null
        _imageUploadProgress.value = null
        _reconstructionProgress.value = null
        _photocollectionReady.value = false
        Log.d(TAG, "Photocollection reset complete")
    }

    private fun deletePhotocollection() {
        currentPhotocollection?.let { collection ->
            scope.launch {
                deletePhotocollection(collection.id)
            }
        }
    }

    suspend fun deletePhotocollection(id: Long): Boolean {
        Log.d(TAG, "Deleting photocollection: $id")
        val result = safeCall { photocollectionService.deletePhotocollection(id) }
        return when (result) {
            is Result.Success -> {
                Log.d(TAG, "Photocollection deleted: $id")
                true
            }

            else -> {
                Log.e(TAG, "Failed to delete photocollection: $result")
                false
            }
        }
    }

    fun getImagesDir(referenceNumber: String) = File(
        getModelsDir(),
        referenceNumber
    )

    companion object {
        private val TAG = ReconstructionRepository::class.simpleName
        private const val POLLING_INTERVAL_MS = 5000L // Poll every 5 seconds as fallback
    }
}

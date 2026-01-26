package health.openwater.openlifu3dscanner.network.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.ImageUploader
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.PhotoscanService
import health.openwater.openlifu3dscanner.network.api.WebsocketService
import health.openwater.openlifu3dscanner.network.dto.CreatePhotocollectionRequest
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.Photoscan
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.network.dto.StartPhotoscanRequest
import health.openwater.openlifu3dscanner.network.model.DownloadResult
import health.openwater.openlifu3dscanner.network.model.DownloadingItem
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.network.model.Type
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authService: AuthService,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val websocketService: WebsocketService,
    private val userRepository: UserRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentPhotocollection: Photocollection? = null
    private val _photocollectionReady = MutableStateFlow(false)
    val photocollectionReady: StateFlow<Boolean> = _photocollectionReady.asStateFlow()
    private val imageUploadProgressFlow = MutableStateFlow<ImageUploadProgress?>(null)

    // Photo download progress: Pair(downloaded, total)
    private val _photoDownloadProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val photoDownloadProgress: StateFlow<Pair<Int, Int>?> = _photoDownloadProgress.asStateFlow()
    private val reconstructionProgressFlow = MutableStateFlow<ReconstructionProgress?>(null)
    private val downloadResultsFlow = MutableStateFlow<DownloadResult?>(null)

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private var _currentPhotoscanId: Long? = null
    val currentPhotoscanId: Long? get() = _currentPhotoscanId

    private var imageUploader: ImageUploader? = null
    private var autoUpload: Boolean = false

    private val downloadQueue = LinkedBlockingQueue<DownloadingItem>()
    private var downloaderJob: Job? = null

    var currentReferenceNumber: String? = null
        private set

    var totalImageCount: String? = null

    init {
        observeProgress()
    }

    private fun observeProgress() {
        scope.launch {
            imageUploadProgressFlow.collect { progress ->
                if (progress != null) {
                    if (_uploadState.value == UploadState.Idle) {
                        _uploadState.value = UploadState.Uploading
                    }
                    if (progress.uploadedImages == progress.totalImages && progress.totalImages > 0
                        && _uploadState.value == UploadState.Uploading
                    ) {
                        _uploadState.value = UploadState.UploadComplete
                    }
                }
            }
        }

        scope.launch {
            reconstructionProgressFlow.collect { progress ->
                if (progress != null) {
                    when (progress.status) {
                        PhotoscanStatus.STARTED, PhotoscanStatus.RUNNING -> {
                            if (_uploadState.value != UploadState.Reconstructing) {
                                _uploadState.value = UploadState.Reconstructing
                            }
                        }
                        PhotoscanStatus.FINISHED -> {
                            _currentPhotoscanId?.let { stopReconstructionProgressListener(it) }
                        }
                        PhotoscanStatus.FAILED -> {
                            _uploadState.value = UploadState.Error(
                                progress.message ?: "Reconstruction failed"
                            )
                            _currentPhotoscanId?.let { stopReconstructionProgressListener(it) }
                        }
                        PhotoscanStatus.STOPPED, null -> {}
                    }
                }
            }
        }
    }

    fun isLoggedInAndOnline(): Boolean {
        return runBlocking {
            authService.isSignedIn() && userRepository.isCloudAvailable()
        }
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
        autoUpload = false
        currentPhotocollection = null
        totalImageCount = null
        imageUploadProgressFlow.value = null
        reconstructionProgressFlow.value = null
        _photocollectionReady.value = false
        Log.d(TAG, "reset")
    }

    fun getImageUploadProgress() = imageUploadProgressFlow.asStateFlow()
    fun getReconstructionProgress() = reconstructionProgressFlow.asStateFlow()
    fun getDownloadResultsFlow() = downloadResultsFlow.asStateFlow()

    fun download(item: DownloadingItem) {
        if (!downloadQueue.contains(item)) {
            downloadQueue.add(item)
            startDownloaderJob()
        }
    }

    fun getCurrentPhotocollection(): Photocollection? = currentPhotocollection

    fun createPhotocollection(name: String, autoUpload: Boolean) {
        this.currentReferenceNumber = name
        this.autoUpload = autoUpload
        _uploadState.value = UploadState.Idle
        _photocollectionReady.value = false

        val uid = authService.getCurrentUser()?.uid ?: return

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
                    imageUploadProgressFlow.value = null

                    currentPhotocollection?.id?.let { id ->
                        Log.d(TAG, "Photocollection created: $id")
                        imageUploader = ImageUploader(
                            context = context,
                            photocollectionId = id,
                            imagesDir = getImagesDir(name),
                            imageUploadProgressFlow = imageUploadProgressFlow,
                            photocollectionService = photocollectionService,
                            scope = scope
                        )
                        _photocollectionReady.value = true
                        if (autoUpload) {
                            imageUploader?.start(autoUpload = true)
                        }
                    }
                }
                else -> Log.e(TAG, "Can't create photo collection: $result")
            }
        }
    }

    fun deletePhotocollection() {
        currentPhotocollection?.let {
            scope.launch {
                deletePhotocollection(it.id)
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

    suspend fun deletePhotoscan(id: Long): Boolean {
        Log.d(TAG, "Deleting photoscan: $id")
        val result = safeCall { photoscanService.deletePhotoscan(id) }
        return when (result) {
            is Result.Success -> {
                Log.d(TAG, "Photoscan deleted: $id")
                true
            }
            else -> {
                Log.e(TAG, "Failed to delete photoscan: $result")
                false
            }
        }
    }

    fun deleteLocalScanDirectory(collectionName: String): Boolean {
        val dir = getImagesDir(collectionName)
        return if (dir.exists()) {
            Log.d(TAG, "Deleting local directory: $dir")
            dir.deleteRecursively()
        } else {
            Log.d(TAG, "Local directory does not exist: $dir")
            true
        }
    }

    fun onImageCaptured() {
        imageUploader?.onImageCaptured()
    }

    fun uploadRemainingPhotos() {
        imageUploader?.start(autoUpload = false)
    }

    fun stopImageUploader() {
        imageUploader?.stop()
        imageUploader = null
    }

    fun setStartingReconstruction() {
        _uploadState.value = UploadState.StartingReconstruction
    }

    suspend fun startReconstructionFlow(): Long? {
        val photoscanId = startReconstruction()
        if (photoscanId != null) {
            _currentPhotoscanId = photoscanId
            startReconstructionProgressListener(photoscanId)
            _uploadState.value = UploadState.Reconstructing
        } else {
            _uploadState.value = UploadState.Error("Failed to start reconstruction")
        }
        return photoscanId
    }

    fun reset(removeLocalCollection: Boolean) {
        _currentPhotoscanId?.let { stopReconstructionProgressListener(it) }
        _currentPhotoscanId = null
        if (removeLocalCollection) {
            resetCurrentPhotocollection()
        } else {
            // Reset photocollection state without deleting local files
            imageUploader?.stop()
            imageUploader = null
            currentPhotocollection = null
            _photocollectionReady.value = false
        }
        _uploadState.value = UploadState.Idle
        imageUploadProgressFlow.value = null
        reconstructionProgressFlow.value = null
    }

    private suspend fun startReconstruction(): Long? {
        val id = currentPhotocollection?.id ?: return null
        return startReconstruction(id)
    }

    private suspend fun startReconstruction(collectionId: Long): Long? {
        val result = safeCall {
            photocollectionService.startPhotoscan(collectionId, StartPhotoscanRequest())
        }
        return when (result) {
            is Result.Success -> result.body.photoscanId
            else -> {
                Log.e(TAG, "Failed to start reconstruction: $result")
                null
            }
        }
    }

    suspend fun startReconstructionProgressListener(photoscanId: Long): Photoscan? {
        val photoscan = getPhotoscan(photoscanId) ?: return null
        reconstructionProgressFlow.emit(
            ReconstructionProgress(
                photoscan.progress, photoscan.message, photoscan.status
            )
        )
        websocketService.connect(photoscanId, reconstructionProgressFlow)
        return photoscan
    }

    fun stopReconstructionProgressListener(photoscanId: Long) {
        websocketService.disconnect(photoscanId)
    }

    suspend fun getPhotocollection(
        id: Long,
        joinPhotos: Boolean = false,
        joinCoordinates: Boolean = false
    ): Photocollection? {
        val result = safeCall {
            photocollectionService.getPhotocollection(id, joinPhotos, joinCoordinates)
        }
        return when (result) {
            is Result.Success -> result.body
            else -> {
                Log.e(TAG, "Failed to get photocollection: $result")
                null
            }
        }
    }

    suspend fun getPhotoscan(id: Long): Photoscan? {
        val result = safeCall { photoscanService.getPhotoscan(id) }
        return when (result) {
            is Result.Success -> result.body
            else -> {
                Log.e(TAG, "Failed to get photoscan: $result")
                null
            }
        }
    }

    suspend fun downloadPhotoscan(id: Long, outputDir: File): Boolean {
        try {
            val response = photoscanService.getMesh(id)

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val dir = File(outputDir, SCAN_DIR)
                    dir.mkdirs()
                    extractZip(body, dir)
                    Log.d(TAG, "Photoscan saved to ${dir.absolutePath}")
                    return true
                }
            } else {
                Log.e(TAG, "Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun getImagesDir(referenceNumber: String) = File(
        getModelsDir(),
        referenceNumber
    )

    private fun extractZip(body: ResponseBody, dir: File) {
        val zis = ZipInputStream(BufferedInputStream(body.byteStream()))
        val buffer = ByteArray(8192)

        while (true) {
            val ze = zis.nextEntry ?: break
            val filename = ze.name
            val file = File(dir, filename)

            if (ze.isDirectory) {
                file.mkdirs()
            } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { out ->
                    var count: Int
                    while (zis.read(buffer).also { count = it } != -1) {
                        out.write(buffer, 0, count)
                    }
                }
            }
            zis.closeEntry()
        }
        zis.close()
    }

    private fun saveResponseBodyToDisk(body: ResponseBody, file: File) {
        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }
    }

    private fun startDownloaderJob() {
        if (downloaderJob == null || downloaderJob?.isCompleted == true) {
            downloaderJob = scope.launch {
                while (isActive && downloadQueue.isNotEmpty()) {
                    val item = downloadQueue.poll() ?: return@launch
                    val success = when (item.type) {
                        Type.PHOTOCOLLECTION -> downloadPhotocollection(item.id)
                        Type.PHOTOSCAN -> downloadPhotoscan(item.id)
                    }
                    downloadResultsFlow.emit(DownloadResult(item, success))
                }
            }
        }
    }

    private suspend fun downloadPhotocollection(id: Long): Boolean {
        val photocollection = getPhotocollection(id, joinPhotos = true) ?: return false

        val photos = photocollection.photos ?: listOf()
        val outputDir = getImagesDir(photocollection.name ?: return false)
        if (!outputDir.exists()) outputDir.mkdirs()

        val totalPhotos = photos.size
        _photoDownloadProgress.value = Pair(0, totalPhotos)

        for ((index, photo) in photos.withIndex()) {
            Log.d(TAG, "Loading photo ${photo.fileName}")
            try {
                val photoResponse =
                    photocollectionService.downloadPhoto(photocollection.id, photo.fileName)
                if (!photoResponse.isSuccessful) {
                    _photoDownloadProgress.value = null
                    return false
                }

                photoResponse.body()?.let {
                    val file = File(outputDir, photo.fileName)
                    saveResponseBodyToDisk(it, file)
                    Log.d(TAG, "Photo saved to ${file.absolutePath}")
                    _photoDownloadProgress.value = Pair(index + 1, totalPhotos)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for ${photo.fileName}", e)
                _photoDownloadProgress.value = null
                return false
            }
        }
        _photoDownloadProgress.value = null
        return true
    }

    private suspend fun downloadPhotoscan(id: Long): Boolean {
        val photoscan = getPhotoscan(id) ?: return false
        val photocollection = getPhotocollection(photoscan.photocollectionId) ?: return false
        val outputDir = getImagesDir(photocollection.name ?: return false)
        if (!outputDir.exists()) outputDir.mkdirs()
        return downloadPhotoscan(photoscan.id, outputDir)
    }

    companion object {
        private val TAG = CloudRepository::class.simpleName
        const val SCAN_DIR = "scan"
    }

}
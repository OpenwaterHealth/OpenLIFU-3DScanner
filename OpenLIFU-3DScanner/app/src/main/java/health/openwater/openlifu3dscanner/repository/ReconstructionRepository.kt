package health.openwater.openlifu3dscanner.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
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
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.network.safeCall
import health.openwater.openlifu3dscanner.service.ReconstructionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class ScanConfig(
    val collectionName: String,
    val autoUploadEnabled: Boolean,
    val sessionId: Long?
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    // Notification IDs for sessions; 1001 is the main service notification
    private val nextNotificationId = AtomicInteger(1002)

    private val _currentSession = MutableStateFlow<ScanSession?>(null)

    // Flat-mapped flows — always reflect the current session's state
    val uploadState: StateFlow<UploadState> = _currentSession
        .flatMapLatest { it?.uploadState ?: flowOf(UploadState.Idle) }
        .stateIn(scope, SharingStarted.Eagerly, UploadState.Idle)

    val imageUploadProgress: StateFlow<ImageUploadProgress?> = _currentSession
        .flatMapLatest { it?.imageUploadProgress ?: flowOf(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val reconstructionProgress: StateFlow<ReconstructionProgress?> = _currentSession
        .flatMapLatest { it?.reconstructionProgress ?: flowOf(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val photocollectionReady: StateFlow<Boolean> = _currentSession
        .flatMapLatest { it?.photocollectionReady ?: flowOf(false) }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val currentPhotoscanId: Long? get() = _currentSession.value?.currentPhotoscanId

    var scanConfig: ScanConfig? = null
        private set

    fun setScanConfig(config: ScanConfig) {
        scanConfig = config
    }

    fun isLoggedInAndOnline(): Boolean = runBlocking {
        authService.isSignedIn() && userRepository.isCloudAvailable()
    }

    fun getCurrentPhotocollection(): Photocollection? = _currentSession.value?.collection

    /**
     * Dismiss the current session without starting a new one.
     * - If it was idle/errored: stop and delete from server + local.
     * - If it was in-progress: move to background with notifications.
     * Call this when entering the scanner while offline (no new session will be created).
     */
    fun dismissCurrentSession() {
        val session = _currentSession.value ?: return
        _currentSession.value = null
        val keptAlive = session.runInBackground()
        if (!keptAlive) {
            deleteSessionData(session)
        }
    }

    /**
     * Create a new photocollection and session. The previous session (if any) is
     * automatically moved to background. If [cancelPrevious] is true the previous
     * session is cancelled and its data deleted instead.
     */
    fun createPhotocollection(
        name: String,
        sessionId: Long?,
        autoUpload: Boolean,
        cancelPrevious: Boolean = false
    ) {
        Log.d(TAG, "Creating photocollection: $name, autoUpload=$autoUpload, cancelPrevious=$cancelPrevious")

        val oldSession = _currentSession.value
        _currentSession.value = null

        if (oldSession != null) {
            if (cancelPrevious) {
                oldSession.stop()
                deleteSessionData(oldSession)
            } else {
                val keptAlive = oldSession.runInBackground()
                if (!keptAlive) deleteSessionData(oldSession)
            }
        }

        val uid = authService.getCurrentUser()?.uid
        if (uid == null) {
            Log.e(TAG, "User not logged in")
            return
        }

        ReconstructionService.start(context)

        scope.launch {
            val result = safeCall {
                photocollectionService.createPhotocollection(
                    CreatePhotocollectionRequest(
                        accountId = uid,
                        name = name,
                        sessionId = sessionId
                    )
                )
            }

            when (result) {
                is Result.Success -> {
                    val session = ScanSession(
                        collectionName = name,
                        collection = result.body,
                        imagesDir = getImagesDir(name),
                        notificationId = nextNotificationId.getAndIncrement(),
                        context = context,
                        photocollectionService = photocollectionService,
                        photoscanService = photoscanService,
                        websocketService = websocketService,
                        parentScope = scope
                    )
                    _currentSession.value = session
                    if (autoUpload) session.startAutoUpload()
                }

                is Result.NetworkError -> {
                    Log.e(TAG, "Network error: ${result.message}")
                    ReconstructionService.stop(context)
                }

                is Result.AuthError -> {
                    Log.e(TAG, "Auth error creating photocollection")
                    ReconstructionService.stop(context)
                }

                is Result.ServerError -> {
                    Log.e(TAG, "Server error ${result.code}: ${result.message}")
                    ReconstructionService.stop(context)
                }

                is Result.UnexpectedError -> {
                    Log.e(TAG, "Unexpected error: ${result.message}")
                    ReconstructionService.stop(context)
                }
            }
        }
    }

    fun onImageCaptured() = _currentSession.value?.onImageCaptured()

    fun onScanComplete() = _currentSession.value?.onScanComplete()

    fun uploadRemainingPhotos() = _currentSession.value?.startManualUpload()

    fun startReconstruction() = _currentSession.value?.startReconstruction()

    /**
     * Reset: stops the current session and optionally deletes its data.
     * @param removeLocalCollection If true, deletes server collection and local files
     *                              (skipped if reconstruction has already started).
     */
    fun reset(removeLocalCollection: Boolean) {
        Log.d(TAG, "Reset, removeLocalCollection=$removeLocalCollection")
        val session = _currentSession.value
        _currentSession.value = null
        if (session != null) {
            session.stop()
            if (removeLocalCollection) deleteSessionData(session)
        }
        ReconstructionService.stop(context)
    }

    private fun deleteSessionData(session: ScanSession) {
        val state = session.uploadState.value
        val reconstructionStarted = state is UploadState.StartingReconstruction
                || state is UploadState.Reconstructing
        if (!reconstructionStarted) {
            scope.launch {
                safeCall { photocollectionService.deletePhotocollection(session.collection.id) }
                session.imagesDir.deleteRecursively()
                Log.d(TAG, "Deleted session data for ${session.collectionName}")
            }
        } else {
            Log.d(TAG, "Skipping delete — reconstruction already started for ${session.collectionName}")
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

    fun getImagesDir(referenceNumber: String) = File(getModelsDir(context), referenceNumber)

    companion object {
        private val TAG = ReconstructionRepository::class.simpleName
    }
}

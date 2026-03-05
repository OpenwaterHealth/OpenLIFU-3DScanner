package health.openwater.openlifu3dscanner.repository

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.activity.MainActivity
import health.openwater.openlifu3dscanner.core.ImageUploader
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.PhotoscanService
import health.openwater.openlifu3dscanner.network.api.WebsocketService
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.network.dto.StartPhotoscanRequest
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.network.safeCall
import health.openwater.openlifu3dscanner.service.ReconstructionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Encapsulates all state and logic for a single scan lifecycle:
 * collection creation → upload → reconstruction.
 *
 * Each session has its own coroutine scope (child of the repo's scope), so
 * cancelling a session stops all its coroutines without touching other sessions.
 *
 * When the user starts a new scan, [runInBackground] is called on the current
 * session. If still uploading/reconstructing it continues silently with
 * notifications; if idle/errored it cleans itself up.
 */
class ScanSession(
    val collectionName: String,
    val collection: Photocollection,
    val imagesDir: File,
    private val notificationId: Int,
    private val context: Context,
    private val photocollectionService: PhotocollectionService,
    private val photoscanService: PhotoscanService,
    private val websocketService: WebsocketService,
    parentScope: CoroutineScope
) {
    private val sessionJob = SupervisorJob(parentScope.coroutineContext[Job])
    private val sessionScope = CoroutineScope(parentScope.coroutineContext + sessionJob)

    val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val imageUploadProgress = MutableStateFlow<ImageUploadProgress?>(null)
    val reconstructionProgress = MutableStateFlow<ReconstructionProgress?>(null)
    val photocollectionReady = MutableStateFlow(true)

    var currentPhotoscanId: Long? = null
        private set

    private val creationTime = System.currentTimeMillis()

    @Volatile
    private var scanComplete = false

    @Volatile
    private var runningInBackground = false

    private var pollingJob: Job? = null

    val imageUploader = ImageUploader(
        context = context,
        photocollectionId = collection.id,
        imagesDir = imagesDir,
        progressFlow = imageUploadProgress,
        photocollectionService = photocollectionService,
        scope = sessionScope
    )

    init {
        observeProgress()
    }

    private fun observeProgress() {
        sessionScope.launch {
            imageUploadProgress.collect { progress ->
                progress ?: return@collect
                Log.d(
                    TAG,
                    "[$collectionName] Upload: ${progress.uploadedImages}/${progress.totalImages}"
                )

                if (uploadState.value == UploadState.Idle && progress.totalImages > 0) {
                    uploadState.value = UploadState.Uploading
                }

                when {
                    progress.failed -> uploadState.value = UploadState.Error("Upload failed")
                    progress.uploadedImages == progress.totalImages && progress.totalImages > 0 -> {
                        if (uploadState.value == UploadState.Uploading && scanComplete) {
                            uploadState.value = UploadState.UploadComplete
                            launchReconstruction()
                        }
                    }
                }

                if (runningInBackground) postUploadNotification(progress)
            }
        }

        sessionScope.launch {
            reconstructionProgress.collect { progress ->
                progress ?: return@collect

                when (progress.status) {
                    PhotoscanStatus.STARTED, PhotoscanStatus.RUNNING -> {
                        if (uploadState.value != UploadState.Reconstructing) {
                            uploadState.value = UploadState.Reconstructing
                        }
                    }

                    PhotoscanStatus.FINISHED -> {
                        currentPhotoscanId?.let { stopProgressListener(it) }
                        if (!runningInBackground) ReconstructionService.stop(context)
                    }

                    PhotoscanStatus.FAILED -> {
                        uploadState.value =
                            UploadState.Error(progress.message ?: "Reconstruction failed")
                        currentPhotoscanId?.let { stopProgressListener(it) }
                        if (!runningInBackground) ReconstructionService.stop(context)
                    }

                    PhotoscanStatus.STOPPED, null -> {}
                }

                if (runningInBackground) {
                    val isTerminal = progress.status == PhotoscanStatus.FINISHED ||
                            progress.status == PhotoscanStatus.FAILED
                    val text = when (progress.status) {
                        PhotoscanStatus.FINISHED -> context.getString(R.string.reconstruction_complete)
                        PhotoscanStatus.FAILED -> context.getString(R.string.reconstruction_failed)
                        PhotoscanStatus.STOPPED -> context.getString(R.string.reconstruction_stopped)
                        else -> progress.message
                            ?: context.getString(R.string.notification_processing)
                    }
                    postSimpleNotification(text, ongoing = !isTerminal)
                    if (isTerminal) sessionJob.cancel()
                }
            }
        }
    }

    fun startAutoUpload() {
        imageUploader.start(autoUpload = true, resetProgress = true)
    }

    fun startManualUpload() {
        if (!imageUploader.isRunning) {
            imageUploader.start(autoUpload = false, resetProgress = false)
        }
    }

    fun onImageCaptured() = imageUploader.onImageCaptured()

    fun onScanComplete() {
        Log.d(TAG, "[$collectionName] Scan marked as complete")
        scanComplete = true
        imageUploadProgress.value?.let { progress ->
            if (progress.uploadedImages == progress.totalImages && progress.totalImages > 0
                && uploadState.value == UploadState.Uploading
            ) {
                uploadState.value = UploadState.UploadComplete
                launchReconstruction()
            }
        }
    }

    fun startReconstruction() {
        val state = uploadState.value
        if (state is UploadState.StartingReconstruction || state is UploadState.Reconstructing) return
        uploadState.value = UploadState.StartingReconstruction
        sessionScope.launch { doStartReconstruction() }
    }

    private fun launchReconstruction() {
        uploadState.value = UploadState.StartingReconstruction
        sessionScope.launch { doStartReconstruction() }
    }

    private suspend fun doStartReconstruction() {
        val result = safeCall {
            photocollectionService.startPhotoscan(collection.id, StartPhotoscanRequest())
        }
        if (result is Result.Success) {
            currentPhotoscanId = result.body.photoscanId
            startProgressListener(result.body.photoscanId)
            uploadState.value = UploadState.Reconstructing
        } else {
            Log.e(TAG, "[$collectionName] Failed to start reconstruction: $result")
            uploadState.value = UploadState.Error("Failed to start reconstruction")
            if (!runningInBackground) ReconstructionService.stop(context)
        }
    }

    private suspend fun startProgressListener(id: Long) {
        val initial = safeCall { photoscanService.getPhotoscan(id) }
        if (initial is Result.Success) {
            reconstructionProgress.emit(
                ReconstructionProgress(
                    initial.body.progress,
                    initial.body.message,
                    initial.body.status
                )
            )
        }
        websocketService.connect(id, reconstructionProgress)

        pollingJob?.cancel()
        pollingJob = sessionScope.launch {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                val result = safeCall { photoscanService.getPhotoscan(id) }
                if (result is Result.Success) {
                    reconstructionProgress.emit(
                        ReconstructionProgress(
                            result.body.progress,
                            result.body.message,
                            result.body.status
                        )
                    )
                    val status = result.body.status
                    if (status == PhotoscanStatus.FINISHED ||
                        status == PhotoscanStatus.FAILED ||
                        status == PhotoscanStatus.STOPPED
                    ) break
                }
            }
        }
    }

    private fun stopProgressListener(id: Long) {
        pollingJob?.cancel()
        pollingJob = null
        websocketService.disconnect(id)
    }

    /**
     * Move this session to background. If the session was idle or errored,
     * it cleans up instead (server + local files deleted by the repository).
     * Returns true if the session will keep running, false if it self-terminated.
     */
    fun runInBackground(): Boolean {
        val state = uploadState.value
        if (state is UploadState.Idle || state is UploadState.Error) {
            stop()
            return false
        }

        runningInBackground = true
        Log.d(TAG, "[$collectionName] Running in background, state=$state")

        // Reconstruction may have already finished in the foreground before we went to background
        // (observeProgress doesn't update uploadState on FINISHED, so it stays at Reconstructing)
        if (state is UploadState.Reconstructing) {
            val reconStatus = reconstructionProgress.value?.status
            if (reconStatus == PhotoscanStatus.FINISHED || reconStatus == PhotoscanStatus.FAILED) {
                val text = if (reconStatus == PhotoscanStatus.FINISHED)
                    context.getString(R.string.reconstruction_complete)
                else
                    context.getString(R.string.reconstruction_failed)
                postSimpleNotification(text, ongoing = false)
                sessionJob.cancel()
                return true
            }
        }

        // Post an immediate notification so the user sees it right away
        when (state) {
            is UploadState.Uploading -> imageUploadProgress.value?.let { postUploadNotification(it) }
            is UploadState.Reconstructing ->
                postSimpleNotification(context.getString(R.string.notification_processing))

            is UploadState.UploadComplete, is UploadState.StartingReconstruction ->
                postSimpleNotification(
                    context.getString(R.string.notification_upload_complete_starting_reconstruction)
                )

            else -> {}
        }
        return true
    }

    /**
     * Stop all coroutines for this session. The repository is responsible for
     * deleting server/local data when needed.
     */
    fun stop() {
        imageUploader.stop()
        currentPhotoscanId?.let { stopProgressListener(it) }
        sessionJob.cancel()
    }

    private fun postUploadNotification(progress: ImageUploadProgress) {
        val text = context.getString(
            R.string.d_of_d_images_uploaded, progress.uploadedImages, progress.totalImages
        )
        notify(buildNotification(text, progress.progress, ongoing = true))
    }

    private fun postSimpleNotification(text: String, ongoing: Boolean = true) {
        notify(buildNotification(text, progress = null, ongoing = ongoing))
    }

    private fun notify(notification: Notification) {
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }

    private fun buildNotification(
        text: String, progress: Int?, ongoing: Boolean
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, "photoscan")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, ReconstructionService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle(collectionName)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setWhen(creationTime)
            .setShowWhen(false)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (progress != null) builder.setProgress(100, progress, false)
        else if (ongoing) builder.setProgress(0, 0, true)
        return builder.build()
    }

    companion object {
        private val TAG = ScanSession::class.simpleName
        private const val POLLING_INTERVAL_MS = 5000L
    }
}

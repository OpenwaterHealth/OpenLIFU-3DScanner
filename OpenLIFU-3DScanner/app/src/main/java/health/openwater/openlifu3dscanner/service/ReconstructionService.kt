package health.openwater.openlifu3dscanner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.activity.MainActivity
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.repository.ReconstructionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReconstructionService : Service() {

    @Inject
    lateinit var reconstructionRepository: ReconstructionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private val TAG = ReconstructionService::class.simpleName
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "upload_channel"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        fun start(context: Context) {
            val intent = Intent(context, ReconstructionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReconstructionService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UploadService created")
        createNotificationChannel()

        // Must call startForeground() immediately in onCreate() to avoid
        // ForegroundServiceDidNotStartInTimeException
        val notification = createNotification(getString(R.string.notification_preparing_upload), null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UploadService started")
        _isRunning.value = true

        // Observe upload progress and update notification
        observeProgress()

        return START_STICKY
    }

    private fun observeProgress() {
        serviceScope.launch {
            reconstructionRepository.uploadState.collect { state ->
                updateNotificationForState(state)
            }
        }

        serviceScope.launch {
            reconstructionRepository.imageUploadProgress.collect { progress ->
                if (progress != null && reconstructionRepository.uploadState.value == UploadState.Uploading) {
                    updateNotificationWithUploadProgress(progress)
                }
            }
        }

        serviceScope.launch {
            reconstructionRepository.reconstructionProgress.collect { progress ->
                if (progress != null && reconstructionRepository.uploadState.value == UploadState.Reconstructing) {
                    updateNotificationWithReconstructionProgress(progress)
                }
            }
        }
    }

    private fun updateNotificationForState(state: UploadState) {
        val notification = when (state) {
            is UploadState.Idle -> createNotification(getString(R.string.notification_preparing), null)
            is UploadState.Uploading -> createNotification(getString(R.string.notification_uploading_images), null)
            is UploadState.UploadComplete -> createNotification(getString(R.string.notification_upload_complete_starting_reconstruction), null)
            is UploadState.StartingReconstruction -> createNotification(getString(R.string.notification_starting_reconstruction), null)
            is UploadState.Reconstructing -> createNotification(getString(R.string.notification_reconstructing_3d_model), null)
            is UploadState.Error -> createNotification(getString(R.string.notification_error, state.message), null)
        }
        updateNotification(notification)
    }

    private fun updateNotificationWithUploadProgress(progress: ImageUploadProgress) {
        val contentText = getString(R.string.d_of_d_images_uploaded, progress.uploadedImages, progress.totalImages)
        updateNotification(createNotification(contentText, progress.progress))
    }

    private fun updateNotificationWithReconstructionProgress(progress: ReconstructionProgress) {
        val message = progress.message ?: getString(R.string.notification_processing)
        val isComplete = progress.status == PhotoscanStatus.FINISHED ||
                progress.status == PhotoscanStatus.FAILED ||
                progress.status == PhotoscanStatus.STOPPED
        updateNotification(createNotification(message, if (isComplete) null else progress.progress, ongoing = !isComplete))
    }

    private fun updateNotification(notification: Notification) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        Log.d(TAG, "UploadService destroyed")
        _isRunning.value = false
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_upload),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_upload_description)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        contentText: String,
        progress: Int?,
        ongoing: Boolean = true
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, "photoscan")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val collectionName = reconstructionRepository.scanConfig?.collectionName
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle(collectionName ?: getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress != null) {
            builder.setProgress(100, progress, false)
        } else if (ongoing) {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "OpenLIFU::UploadWakeLock"
        ).apply {
            acquire(60 * 60 * 1000L) // 60 minutes max for upload + reconstruction
        }
        Log.d(TAG, "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
}

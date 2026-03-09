package health.openwater.openlifu3dscanner.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import java.util.concurrent.TimeUnit

class LocalScanCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        val deleted = getModelsDir(applicationContext)
            .listFiles()
            ?.filter { it.isDirectory && it.lastModified() < cutoff }
            ?.onEach {
                Log.d(TAG, "Deleting stale local scan: ${it.name}")
                it.deleteRecursively()
            }
            ?.size ?: 0
        Log.d(TAG, "Cleanup complete: $deleted directories removed")
        return Result.success()
    }

    companion object {
        private const val TAG = "LocalScanCleanup"
        private const val PERIODIC_WORK_NAME = "local_scan_cleanup_periodic"
        private const val ONETIME_WORK_NAME = "local_scan_cleanup_launch"

        /** Schedule the periodic cleanup (every 1 h). Call once from App.onCreate(). */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<LocalScanCleanupWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Run an immediate cleanup. Call on every app launch. */
        fun runOnLaunch(context: Context) {
            val request = OneTimeWorkRequestBuilder<LocalScanCleanupWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

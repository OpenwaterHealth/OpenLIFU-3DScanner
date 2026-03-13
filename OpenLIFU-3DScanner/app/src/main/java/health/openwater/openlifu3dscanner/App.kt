package health.openwater.openlifu3dscanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import health.openwater.openlifu3dscanner.work.LocalScanCleanupWorker

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        LocalScanCleanupWorker.schedulePeriodic(this)
        LocalScanCleanupWorker.runOnLaunch(this)
    }
}

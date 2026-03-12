package health.openwater.openlifu3dscanner

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp
import health.openwater.openlifu3dscanner.work.LocalScanCleanupWorker

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val prodOptions = FirebaseOptions.Builder()
            .setProjectId(getString(R.string.project_id))
            .setApplicationId(getString(R.string.google_app_id))
            .setApiKey(getString(R.string.google_crash_reporting_api_key))
            .build()
        FirebaseApp.initializeApp(this, prodOptions)

        val devOptions = FirebaseOptions.Builder()
            .setProjectId(getString(R.string.dev_project_id))
            .setApplicationId(getString(R.string.dev_google_app_id))
            .setApiKey(getString(R.string.dev_google_api_key))
            .build()
        FirebaseApp.initializeApp(this, devOptions, "dev")

        LocalScanCleanupWorker.schedulePeriodic(this)
        LocalScanCleanupWorker.runOnLaunch(this)
    }
}
package health.openwater.openlifu3dscanner.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.navigation.AppNavigation
import health.openwater.openlifu3dscanner.theme.HeadScannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingDestination = mutableStateOf<String?>(null)
    private val showUpdateRestartDialog = mutableStateOf(false)

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateRestartDialog.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateListener)

        updateResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { /* no-op: update continues in background regardless of result */ }

        if (savedInstanceState == null) {
            pendingDestination.value = intent.getStringExtra(EXTRA_DESTINATION)
        }

        setContent {
            HeadScannerTheme {
                AppNavigation(
                    pendingDestination = pendingDestination.value,
                    onDestinationHandled = { pendingDestination.value = null }
                )

                if (showUpdateRestartDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showUpdateRestartDialog.value = false },
                        title = { Text(stringResource(R.string.update_ready_title)) },
                        text = { Text(stringResource(R.string.update_ready_message)) },
                        confirmButton = {
                            TextButton(onClick = { appUpdateManager.completeUpdate() }) {
                                Text(stringResource(R.string.update_restart))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUpdateRestartDialog.value = false }) {
                                Text(stringResource(R.string.update_later))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    showUpdateRestartDialog.value = true
                }
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateResultLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(installStateListener)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination.value = intent.getStringExtra(EXTRA_DESTINATION)
    }

    companion object {
        const val EXTRA_DESTINATION = "nav_destination"
    }
}
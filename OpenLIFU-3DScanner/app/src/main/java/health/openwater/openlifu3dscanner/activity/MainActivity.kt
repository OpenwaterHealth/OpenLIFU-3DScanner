package health.openwater.openlifu3dscanner.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.navigation.AppNavigation
import health.openwater.openlifu3dscanner.theme.HeadScannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            pendingDestination.value = intent.getStringExtra(EXTRA_DESTINATION)
        }
        setContent {
            HeadScannerTheme {
                AppNavigation(
                    pendingDestination = pendingDestination.value,
                    onDestinationHandled = { pendingDestination.value = null }
                )
            }
        }
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
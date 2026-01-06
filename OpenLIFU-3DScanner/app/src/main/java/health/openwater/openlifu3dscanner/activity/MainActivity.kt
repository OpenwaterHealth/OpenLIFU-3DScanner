package health.openwater.openlifu3dscanner.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.navigation.AppNavigation
import health.openwater.openlifu3dscanner.theme.HeadScannerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HeadScannerTheme {
                AppNavigation()
            }
        }
    }
}
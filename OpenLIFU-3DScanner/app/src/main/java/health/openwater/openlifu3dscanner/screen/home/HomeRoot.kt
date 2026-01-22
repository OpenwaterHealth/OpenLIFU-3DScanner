package health.openwater.openlifu3dscanner.screen.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.extensions.hasAllFilesAccess
import health.openwater.openlifu3dscanner.extensions.requestAllFilesAccess
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@Composable
fun HomeRoot(
    onStartScan: () -> Unit,
    onViewCollection: () -> Unit,
    onSignIn: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val userViewModel: UserViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()

    var hasStorageAccess by remember { mutableStateOf(hasAllFilesAccess()) }

    LaunchedEffect(Unit) {
        userViewModel.initialize()
        userViewModel.getCredits()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStorageAccess = hasAllFilesAccess()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingScreen()
            }

            !uiState.isLoading && !hasStorageAccess -> {
                StoragePermissionScreen(
                    onGrantPermission = {
                        context.requestAllFilesAccess()
                    }
                )
            }

            else -> {
                WelcomeScreen(
                    onStartScan = onStartScan,
                    onViewCollection = onViewCollection
                )
            }
        }

        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
        }

        UserProfileBadge(
            onSignIn = onSignIn,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

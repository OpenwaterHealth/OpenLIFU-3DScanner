package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import health.openwater.openlifu3dscanner.extensions.hasAllFilesAccess
import health.openwater.openlifu3dscanner.extensions.requestAllFilesAccess
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@Composable
fun HomeRoot(
    onStartScan: () -> Unit,
    onViewCollection: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val userInfo by userViewModel.getUserInfo().collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val isCloudAvailable by userViewModel.getCloudAvailability().collectAsState()


    val isLoggedIn = userInfo != null
    var hasStorageAccess by remember { mutableStateOf(context.hasAllFilesAccess()) }

    LaunchedEffect(Unit) {
        userViewModel.refreshUserInfo()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStorageAccess = context.hasAllFilesAccess()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                LoadingScreen()
            }

            isLoggedIn && !hasStorageAccess -> {
                StoragePermissionScreen(
                    onGrantPermission = {
                        context.requestAllFilesAccess()
                    }
                )
            }

            isLoggedIn -> {
                WelcomeScreen(
                    userInfo = userInfo,
                    onStartScan = onStartScan,
                    onViewCollection = onViewCollection
                )
            }

            else -> {
                LoginScreen(
                    onSignIn = { email, password ->
                        userViewModel.signIn(email, password)
                    },
                    onResetPassword = { email ->
                        userViewModel.resetPassword(email)
                    }
                )
            }
        }

        if (!isLoading) {
            CloudStatusIndicator(
                isAvailable = isCloudAvailable,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }

        if (isLoggedIn) {
            UserAvatar(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }
}

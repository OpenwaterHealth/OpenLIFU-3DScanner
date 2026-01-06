package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@Composable
fun HomeRoot(
    onStartScan: () -> Unit,
    onViewCollection: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val userInfo by userViewModel.getUserInfo().collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val isCloudAvailable by userViewModel.getCloudAvailability().collectAsState()
    val isLoggedIn = userInfo != null

    LaunchedEffect(Unit) {
        userViewModel.refreshUserInfo()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isLoading -> {
                LoadingScreen()
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

        // Cloud availability indicator
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
                    .padding(16.dp),
            )
        }
    }
}


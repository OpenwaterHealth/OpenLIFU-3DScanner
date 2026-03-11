package health.openwater.openlifu3dscanner.screen.home

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import health.openwater.openlifu3dscanner.BuildConfig
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateCollection: () -> Unit,
    onRequestPermissions: () -> Unit,
    onViewCollection: () -> Unit,
    onSettings: () -> Unit,
    onSignIn: () -> Unit,
) {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    if (!view.isInEditMode) {
        DisposableEffect(darkTheme) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            onDispose { controller.isAppearanceLightStatusBars = false }
        }
    }

    var showNoCreditsWarning by remember { mutableStateOf(false) }
    var showNoInternetWarning by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val hasCredits = (uiState.credits ?: 0) > 0

    var showNotice by remember { mutableStateOf(userViewModel.shouldShowNotice) }
    LaunchedEffect(uiState.user) {
        if (userViewModel.shouldShowNotice) {
            showNotice = true
        }
    }

    // Check permissions
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val allPermissionsGranted = cameraPermissionState.status.isGranted &&
            (notificationPermissionState?.status?.isGranted ?: true)

    fun handleStartScan() {
        if (!allPermissionsGranted) {
            onRequestPermissions()
            return
        }
        if (uiState.user != null && uiState.credits == null) {
            showNoInternetWarning = true
        } else if (uiState.user != null && !hasCredits) {
            showNoCreditsWarning = true
        } else {
            onCreateCollection()
        }
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.openwater),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                    )

                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(end = 8.dp, bottom = 4.dp)
                    )
                }

            }
        }
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeRoot(
                onStartScan = { handleStartScan() },
                onSettings = onSettings,
                onViewCollection = onViewCollection,
                onSignIn = onSignIn,
                onSupport = { showSupportDialog = true }
            )
        }
    }

    if (showNoCreditsWarning) {
        NoCreditsModal(
            onDismiss = { showNoCreditsWarning = false },
            onProceedOffline = {
                showNoCreditsWarning = false
                onCreateCollection()
            }
        )
    }

    if (showNoInternetWarning) {
        NoInternetModal(
            onDismiss = { showNoInternetWarning = false },
            onProceedOffline = {
                showNoInternetWarning = false
                onCreateCollection()
            }
        )
    }

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }

    if (showNotice) {
        NoticeDialog(onDismiss = { dontShowAgain ->
            showNotice = false
            if (dontShowAgain) {
                userViewModel.noticeAcknowledged()
            } else {
                userViewModel.noticeDismissed()
            }
        })
    }
}

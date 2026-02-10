package health.openwater.openlifu3dscanner.screen.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import health.openwater.openlifu3dscanner.BuildConfig
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.hasAllFilesAccess
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
    var showNoCreditsWarning by remember { mutableStateOf(false) }
    var showNoInternetWarning by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val hasCredits = (uiState.credits ?: 0) > 0

    // Check permissions
    val storageGranted = hasAllFilesAccess()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    val allPermissionsGranted = storageGranted &&
            cameraPermissionState.status.isGranted &&
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

                IconButton(
                    onClick = { showSupportDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = stringResource(R.string.customer_support)
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
                onSignIn = onSignIn
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
}

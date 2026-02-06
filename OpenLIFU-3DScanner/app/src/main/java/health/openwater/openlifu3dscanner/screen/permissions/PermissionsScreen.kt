package health.openwater.openlifu3dscanner.screen.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.hasAllFilesAccess
import health.openwater.openlifu3dscanner.extensions.requestAllFilesAccess

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Storage permission state (needs lifecycle observer since it's checked via Settings)
    var storageGranted by remember { mutableStateOf(hasAllFilesAccess()) }

    // Re-check storage permission when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                storageGranted = hasAllFilesAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val cameraGranted = cameraPermissionState.status.isGranted
    val notificationGranted = notificationPermissionState?.status?.isGranted ?: true

    // Track if we've requested permissions (to distinguish "never asked" from "permanently denied")
    var cameraRequested by remember { mutableStateOf(false) }
    var notificationRequested by remember { mutableStateOf(false) }

    val allPermissionsGranted = storageGranted && cameraGranted && notificationGranted

    // Auto-navigate when all permissions are granted
    LaunchedEffect(storageGranted, cameraGranted, notificationGranted) {
        if (allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.permissions)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when {
                !storageGranted -> {
                    PermissionRequestContent(
                        title = stringResource(R.string.storage_access_required),
                        message = stringResource(R.string.we_need_access_to_your_media_to_save_and_view_scans),
                        onGrantClick = { context.requestAllFilesAccess() }
                    )
                }

                !cameraGranted -> {
                    // Permanently denied = requested before + no rationale + not granted
                    val permanentlyDenied = cameraRequested &&
                            !cameraPermissionState.status.shouldShowRationale
                    PermissionRequestContent(
                        title = stringResource(R.string.camera_permission_required),
                        message = stringResource(R.string.camera_permission_description),
                        onGrantClick = {
                            if (permanentlyDenied) {
                                openAppSettings()
                            } else {
                                cameraRequested = true
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    )
                }

                !notificationGranted -> {
                    val permState = notificationPermissionState!!
                    // Permanently denied = requested before + no rationale + not granted
                    val permanentlyDenied = notificationRequested &&
                            !permState.status.shouldShowRationale
                    PermissionRequestContent(
                        title = stringResource(R.string.notification_permission_required),
                        message = stringResource(R.string.notification_permission_description),
                        onGrantClick = {
                            if (permanentlyDenied) {
                                openAppSettings()
                            } else {
                                notificationRequested = true
                                permState.launchPermissionRequest()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(
    title: String,
    message: String? = null,
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantClick) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

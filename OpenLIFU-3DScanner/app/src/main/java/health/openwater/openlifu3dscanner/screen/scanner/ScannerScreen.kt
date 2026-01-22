package health.openwater.openlifu3dscanner.screen.scanner

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import health.openwater.openlifu3dscanner.viewmodel.ScannerViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import kotlinx.coroutines.launch

const val MIN_IMAGES_COUNT = 20

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    collectionName: String,
    autoUploadEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: (autoUploadEnabled: Boolean, isLoggedIn: Boolean) -> Unit,
    cloudViewModel: CloudViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    scannerViewModel: ScannerViewModel = hiltViewModel()
) {
    val capturedBucketsCount by scannerViewModel.capturedBucketsCount.collectAsState(initial = 0)

    val userInfo by userViewModel.getUserInfo().collectAsState(initial = null)
    val isLoggedIn = userInfo != null

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isScanning by scannerViewModel.isScanning.collectAsState()

    LaunchedEffect(Unit) {
        cloudViewModel.reset(false)

        if (cloudViewModel.isLoggedInAndOnline()) {
            cloudViewModel.createPhotocollection(collectionName, autoUploadEnabled)
        }
    }

    KeepScreenOn()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.scanning)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = !isScanning,
                        modifier = Modifier.alpha(if (!isScanning) 1f else 0.5f),
                        onClick = {
                            if (capturedBucketsCount < MIN_IMAGES_COUNT) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(
                                            R.string.you_need_to_capture_at_least_d_images_to_proceed,
                                            MIN_IMAGES_COUNT
                                        ),
                                        actionLabel = context.getString(R.string.dismiss)
                                    )
                                }
                            } else {
                                onNavigateToProcessing(autoUploadEnabled, isLoggedIn)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.proceed).uppercase(),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary
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

        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

        LaunchedEffect(Unit) {
            if (!cameraPermissionState.status.isGranted) {
                cameraPermissionState.launchPermissionRequest()
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (cameraPermissionState.status.isGranted) {
                ScannerComponent(
                    collectionName = collectionName,
                    onProceed = {
                        onNavigateToProcessing(autoUploadEnabled, isLoggedIn)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }

}

package health.openwater.openlifu3dscanner.screen.scanner

import android.Manifest
import android.util.Log
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    collectionName: String,
    autoUploadEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: (autoUploadEnabled: Boolean) -> Unit,
    cloudViewModel: CloudViewModel = hiltViewModel(),
) {
    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.w("ScannerScreen", "Starting scan with collectionName: $collectionName, $autoUploadEnabled")
        cloudViewModel.start(collectionName, autoUploadEnabled)
    }

    KeepScreenOn()

    Scaffold(
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
                        enabled = completed,
                        modifier = Modifier.alpha(if (completed) 1f else 0.5f),
                        onClick = {
                            onNavigateToProcessing(autoUploadEnabled)
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
                    onComplete = {
                        completed = true
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.camera_permission_required),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

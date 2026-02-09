package health.openwater.openlifu3dscanner.screen.uploading

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.network.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.network.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UploadingRoot(
    onNavigateBack: () -> Unit,
    onViewModel: (scanId: Long, photocollectionId: Long, collectionName: String) -> Unit,
    cloudViewModel: CloudViewModel = hiltViewModel()
) {
    val collectionName = cloudViewModel.scanConfig?.collectionName ?: ""
    val sessionId = cloudViewModel.scanConfig?.sessionId
    // Request notification permission for Android 13+ (fallback if not already granted in ScannerScreen)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState =
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(notificationPermissionState.status.isGranted) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    val scanDir =
        remember(collectionName) { File(getModelsDir(), collectionName) }

    val imageFiles = remember(scanDir) {
        scanDir
            .listFiles { f -> f.extension.equals("jpg", true) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    val uploadState by cloudViewModel.uploadState.collectAsState()
    val imageUploadProgress by cloudViewModel.imageUploadProgress.collectAsState()
    val reconstructionProgress by cloudViewModel.reconstructionProgress.collectAsState()
    val photocollectionReady by cloudViewModel.photocollectionReady.collectAsState()

    // Initialize upload for this collection
    LaunchedEffect(collectionName) {
        if (imageFiles.isEmpty()) return@LaunchedEffect

        val currentCollection = cloudViewModel.getCurrentPhotocollection()

        when {
            // No collection exists - create new one
            currentCollection == null -> {
                cloudViewModel.reset(false)
                cloudViewModel.createPhotocollection(collectionName, autoUpload = false, sessionId = sessionId)
            }
            // Different collection - reset and create new
            currentCollection.name != collectionName -> {
                cloudViewModel.reset(false)
                cloudViewModel.createPhotocollection(collectionName, autoUpload = false, sessionId = sessionId)
            }
            // Same collection exists - just start/continue upload
            else -> {
                cloudViewModel.uploadRemainingPhotos()
            }
        }

        // Mark scan as complete since we're uploading from collections
        // (all images are already captured)
        cloudViewModel.onScanComplete()
    }

    // Start uploading once the photocollection is ready (for new collections)
    LaunchedEffect(photocollectionReady) {
        if (photocollectionReady && imageFiles.isNotEmpty()) {
            // This will be a no-op if already running (checked in ImageUploader)
            cloudViewModel.uploadRemainingPhotos()
            // Ensure scan is marked complete for auto-reconstruction
            cloudViewModel.onScanComplete()
        }
    }

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is UploadState.UploadComplete -> cloudViewModel.startReconstruction()
            else -> {}
        }
    }

    LaunchedEffect(reconstructionProgress?.status) {
        if (reconstructionProgress?.status == PhotoscanStatus.FINISHED) {
            cloudViewModel.currentPhotoscanId?.let { photoscanId ->
                cloudViewModel.getCurrentPhotocollection()?.id?.let { photocollectionId ->
                    onViewModel(photoscanId, photocollectionId, collectionName)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uploadState) {
            is UploadState.Idle,
            is UploadState.Uploading,
            is UploadState.UploadComplete,
            is UploadState.StartingReconstruction -> {
                UploadingView(
                    progress = imageUploadProgress,
                    totalImages = imageFiles.size
                )
            }

            is UploadState.Reconstructing -> {
                ReconstructingView(progress = reconstructionProgress)
            }

            is UploadState.Error -> {
                ErrorView(
                    message = (uploadState as UploadState.Error).message,
                    onRetry = { cloudViewModel.startReconstruction() },
                    onCancel = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun UploadingView(
    progress: ImageUploadProgress?,
    totalImages: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.uploading_images),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        val uploadedCount = progress?.uploadedImages ?: 0
        val total = progress?.totalImages ?: totalImages

        Text(
            text = stringResource(R.string.d_of_d_images_uploaded, uploadedCount, total),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        val animatedProgress by animateFloatAsState(
            targetValue = if (total > 0) uploadedCount.toFloat() / total.toFloat() else 0f,
            label = "upload_progress"
        )

        LinearProgressIndicator(
            drawStopIndicator = { },
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ReconstructingView(
    progress: ReconstructionProgress?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CropRotate,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.reconstructing_3d_model),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        progress?.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val reconstructionProgress = (progress?.progress?.toFloat() ?: 0f) / 100f
        val animatedProgress by animateFloatAsState(
            targetValue = reconstructionProgress,
            label = "reconstruction_progress"
        )

        LinearProgressIndicator(
            drawStopIndicator = { },
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.the_reconstruction_will_continue_running_in_the_cloud_you_may_safely_close_the_app_and_download_the_results_once_complete),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.error),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.cancel))
            }

            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
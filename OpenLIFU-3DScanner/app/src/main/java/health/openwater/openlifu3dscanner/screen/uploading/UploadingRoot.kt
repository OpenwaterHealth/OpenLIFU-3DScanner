package health.openwater.openlifu3dscanner.screen.uploading

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.api.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadingRoot(
    collectionName: String,
    onNavigateBack: () -> Unit,
    onComplete: () -> Unit,
    cloudViewModel: CloudViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scanDir =
        remember(collectionName) { File(context.getExternalFilesDir(null), collectionName) }

    val imageFiles = remember(scanDir) {
        scanDir
            .listFiles { f -> f.extension.equals("jpg", true) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    val uploadState by cloudViewModel.uploadState.collectAsState()
    val imageUploadProgress by cloudViewModel.imageUploadProgress.collectAsState()
    val reconstructionProgress by cloudViewModel.reconstructionProgress.collectAsState()

    LaunchedEffect(imageFiles) {
        if (imageFiles.isNotEmpty()) {
            cloudViewModel.start(collectionName)
        }
    }

    LaunchedEffect(uploadState) {
        Log.w("TAG", "Upload state changed: $uploadState")
        if (uploadState is UploadState.Reconstructing) {
            onComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uploadState) {
            is UploadState.Idle -> {
                IdleView(imageCount = imageFiles.size)
            }

            is UploadState.Uploading -> {
                UploadingView(
                    progress = imageUploadProgress,
                    totalImages = imageFiles.size
                )
            }

            is UploadState.UploadComplete -> {
                UploadCompleteView(
                    onStartReconstruction = { cloudViewModel.startReconstruction() }
                )
            }

            is UploadState.StartingReconstruction -> {
                StartingReconstructionView()
            }

            is UploadState.Reconstructing -> {
                ReconstructingView(progress = reconstructionProgress)
            }

            is UploadState.ReconstructionComplete -> {
                ReconstructionCompleteView(
                    onComplete = onNavigateBack
                )
            }

            is UploadState.Error -> {
                ErrorView(
                    message = (uploadState as UploadState.Error).message,
                    onRetry = { cloudViewModel.uploadRemainingPhotos() },
                    onCancel = {
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

@Composable
fun IdleView(imageCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.preparing_d_images_for_upload, imageCount),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun UploadingView(
    progress: health.openwater.openlifu3dscanner.api.model.ImageUploadProgress?,
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
fun UploadCompleteView(onStartReconstruction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.upload_complete),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.all_images_have_been_uploaded_successfully),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartReconstruction,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CropRotate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.start_3d_reconstruction),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun StartingReconstructionView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.starting_reconstruction),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ReconstructingView(
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
fun ReconstructionCompleteView(onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.reconstruction_complete),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.your_3d_model_has_been_successfully_created),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.done),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ErrorView(
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
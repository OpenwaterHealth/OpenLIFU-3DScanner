package health.openwater.openlifu3dscanner.screen.photoscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.extensions.SCAN_SUBDIR
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.extensions.hasLocalModel
import androidx.compose.ui.platform.LocalContext
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ModelTab(
    collectionName: String,
    photoscanId: Long,
    photocollectionId: Long,
    isLoggedIn: Boolean = false,
    onStartProcessing: (() -> Unit)? = null,
    onTransferToPc: (() -> Unit)? = null,
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Init) }
    val scanDir =
        remember(collectionName) { File(getModelsDir(context), "$collectionName/$SCAN_SUBDIR") }
    val isLocalOnly = photoscanId == 0L

    LaunchedEffect(photocollectionId, isLocalOnly, photoscanId) {
        if (isLocalOnly) {
            // Local-only scan - check if we have a model file
            if (hasLocalModel(context, collectionName)) {
                downloadState = DownloadState.Offline // Has local model, show it
            } else {
                downloadState = DownloadState.NotProcessed // No model, needs processing
            }
        } else {
            // First check if we have a local model
            if (hasLocalModel(context, collectionName)) {
                downloadState = DownloadState.Success
                return@LaunchedEffect
            }

            // Fetch photoscan to check its status
            val photoscan = collectionViewModel.getPhotoscan(photoscanId)

            when (photoscan?.status) {
                PhotoscanStatus.FINISHED -> {
                    // Model is ready, download it
                    downloadState = DownloadState.Downloading
                    collectionViewModel.downloadMesh(photoscanId)
                }

                PhotoscanStatus.STARTED, PhotoscanStatus.RUNNING -> {
                    // Still processing
                    downloadState = DownloadState.Processing
                }

                PhotoscanStatus.FAILED -> {
                    // Processing failed
                    downloadState = DownloadState.Failed
                }

                PhotoscanStatus.STOPPED, null -> {
                    // Not processed or stopped
                    downloadState = DownloadState.NotProcessed
                }
            }
        }
    }

    LaunchedEffect(photoscanId) {
        collectionViewModel.getDownloadResultsFlow().collectLatest { result ->
            if (result?.item?.id == photoscanId) {
                if (result.success) {
                    val collection = collectionViewModel.getPhotocollection(photocollectionId)
                    val name = collection?.name
                    downloadState = if (name != null && hasLocalModel(context, name)) {
                        DownloadState.Success
                    } else {
                        DownloadState.Failed
                    }
                } else {
                    downloadState = DownloadState.Failed
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Use key to force recomposition when state changes
        key(downloadState) {
            when (downloadState) {
                is DownloadState.Init -> Unit

                is DownloadState.Downloading -> {
                    StateDownloadingModel()
                }

                is DownloadState.Offline,
                is DownloadState.Success -> {
                    var rendererReady by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                ModelSurfaceView(context, scanDir.absolutePath) {
                                    rendererReady = true
                                }
                            }
                        )

                        if (!rendererReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                is DownloadState.Failed -> {
                    StateFailed(onAction = {
                        downloadState = DownloadState.Downloading
                        collectionViewModel.downloadMesh(photoscanId)
                    })
                }

                is DownloadState.Processing -> {
                    StateProcessing()
                }

                is DownloadState.NotProcessed -> {
                    StateNotProcessed(
                        isLoggedIn = isLoggedIn,
                        onStartProcessing = onStartProcessing,
                        onTransferToPc = onTransferToPc,
                    )
                }
            }
        }
    }
}


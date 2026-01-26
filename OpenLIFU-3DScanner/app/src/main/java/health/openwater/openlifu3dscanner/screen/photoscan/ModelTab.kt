package health.openwater.openlifu3dscanner.screen.photoscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.extensions.getModelsDir
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
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Init) }
    val scanDir =
        remember(collectionName) { File(getModelsDir(), "${collectionName}/scan") }
    val isLocalOnly = photoscanId == 0L

    LaunchedEffect(photocollectionId, isLocalOnly) {
        if (isLocalOnly) {
            // Local-only scan - check if we have a model file
            val dir = File(getModelsDir(), "${collectionName}/scan")
            if (dir.exists() && dir.listFiles()?.find { it.name == "texturedMesh.obj" } != null) {
                downloadState = DownloadState.Offline // Has local model, show it
            } else {
                downloadState = DownloadState.NotProcessed // No model, needs processing
            }
        } else {
            val collection =
                collectionViewModel.getPhotocollection(
                    photocollectionId = photocollectionId,
                    joinPhotos = true
                )

            if (collection != null) {
                val dir = File(getModelsDir(), "${collectionName}/scan")

                if (dir.exists() && dir.listFiles()?.find { it.name == "texturedMesh.obj" } != null) {
                    downloadState = DownloadState.Success
                } else {
                    // Auto-download the mesh
                    downloadState = DownloadState.Downloading
                    collectionViewModel.downloadMesh(photoscanId)
                }
            } else {
                downloadState = DownloadState.Offline
            }
        }
    }

    LaunchedEffect(photoscanId) {
        collectionViewModel.getDownloadResultsFlow().collectLatest { result ->
            if (result?.item?.id == photoscanId) {
                if (result.success) {
                    val collection = collectionViewModel.getPhotocollection(photocollectionId)
                    if (collection != null) {
                        val dir = File(getModelsDir(), "${collection.name}/scan")
                        if (dir.exists() && dir.listFiles()
                                ?.find { it.name == "texturedMesh.obj" } != null
                        ) {
                            downloadState = DownloadState.Success
                        } else {
                            downloadState = DownloadState.Failed
                        }
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                ModelSurfaceView(context, scanDir.absolutePath)
                            }
                        )
                    }
                }

                is DownloadState.Failed -> {
                    StateFailed(onAction = {
                        downloadState = DownloadState.Downloading
                        collectionViewModel.downloadMesh(photoscanId)
                    })
                }

                is DownloadState.Processing -> {

                }

                is DownloadState.NotProcessed -> {
                    StateNotProcessed(
                        isLoggedIn = isLoggedIn,
                        onStartProcessing = onStartProcessing
                    )
                }
            }
        }
    }
}


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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotosTab(
    collectionName: String,
    photoscanId: Long,
    photocollectionId: Long,
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Init) }
    val scanDir =
        remember(collectionName) { File(getModelsDir(), collectionName) }
    val downloadProgress by collectionViewModel.getPhotoDownloadProgress().collectAsStateWithLifecycle()

    LaunchedEffect(photoscanId) {
        val collection =
            collectionViewModel.getPhotocollection(
                photocollectionId = photocollectionId,
                joinPhotos = true
            )

        if (collection != null) {
            val dir = File(getModelsDir(), collectionName)

            if (dir.exists() && (dir.listFiles()?.size ?: 0) >= (collection.photos?.size ?: 0)) {
                downloadState = DownloadState.Success
            } else {
                // Auto-download the photos
                downloadState = DownloadState.Downloading
                collectionViewModel.downloadPhotocollection(photocollectionId)
            }
        } else {
            downloadState = DownloadState.Offline
        }
    }

    LaunchedEffect(photocollectionId) {
        collectionViewModel.getDownloadResultsFlow().collectLatest { result ->
            if (result?.item?.id == photocollectionId) {
                if (result.success) {
                    val collection = collectionViewModel.getPhotocollection(photocollectionId)
                    if (collection != null) {
                        val dir = File(getModelsDir(), "${collection.name}")
                        if (dir.exists() && (dir.listFiles()?.size ?: 0) > (collection.photos?.size
                                ?: 0)
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
        key(downloadState, downloadProgress) {
            when (downloadState) {
                is DownloadState.Init -> Unit

                is DownloadState.Downloading -> {
                    StateDownloadingPhotos(progress = downloadProgress)
                }

                is DownloadState.Offline,
                is DownloadState.NotProcessed,
                is DownloadState.Success -> {
                    PhotosView(scanDir.absolutePath)
                }

                is DownloadState.Failed -> {
                    StateFailed(onAction = {
                        downloadState = DownloadState.Downloading
                        collectionViewModel.downloadPhotocollection(photocollectionId)
                    })
                }

                is DownloadState.Processing -> {}
            }
        }
    }
}


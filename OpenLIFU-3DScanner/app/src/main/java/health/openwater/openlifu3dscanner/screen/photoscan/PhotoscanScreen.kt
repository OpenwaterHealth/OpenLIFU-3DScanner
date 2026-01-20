package health.openwater.openlifu3dscanner.screen.photoscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoscanScreen(
    scanId: Long,
    autoDownloadEnabled: Boolean,
    onNavigateBack: () -> Unit,
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var modelPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scanId) {
        val collection = collectionViewModel.getPhotocollection(scanId)
        if (collection != null) {
            val dir = File(getModelsDir(), "${collection.name}/scan")

            if (dir.exists()) {
                modelPath = dir.absolutePath
                downloadState = DownloadState.Success
            } else {
                modelPath = null
                downloadState = DownloadState.Idle
                if (autoDownloadEnabled) {
                    downloadState = DownloadState.Downloading
                    collectionViewModel.downloadMesh(scanId)
                }
            }
        }
    }

    LaunchedEffect(scanId) {
        collectionViewModel.getDownloadResultsFlow().collectLatest { result ->
            if (result?.item?.id == scanId) {
                if (result.success) {
                    val collection = collectionViewModel.getPhotocollection(scanId)
                    if (collection != null) {
                        val dir = File(getModelsDir(), "${collection.name}/scan")
                        if (dir.exists()) {
                            modelPath = dir.absolutePath
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Photoscan #$scanId")
                }, navigationIcon = {
                    IconButton(onClick = {
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Use key to force recomposition when state changes
            key(downloadState, modelPath) {
                when (downloadState) {
                    is DownloadState.Idle -> {
                        StateIdle(onAction = {
                            downloadState = DownloadState.Downloading
                            collectionViewModel.downloadMesh(scanId)
                        })
                    }

                    is DownloadState.Downloading -> {
                        StateDownloading()
                    }

                    is DownloadState.Success -> {
                        modelPath?.let { path ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { context ->
                                        ModelSurfaceView(context, path)
                                    }
                                )
                            }
                        } ?: run {
                            // Fallback if path is null
                            StateIdle(onAction = {
                                downloadState = DownloadState.Downloading
                                collectionViewModel.downloadMesh(scanId)
                            })
                        }
                    }

                    is DownloadState.Failed -> {
                        StateFailed(onAction = {
                            downloadState = DownloadState.Downloading
                            collectionViewModel.downloadMesh(scanId)
                        })
                    }
                }
            }
        }
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Success : DownloadState()
    object Failed : DownloadState()
}

@Composable
fun StateFailed(onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.download_failed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.an_error_occurred_while_downloading_the_mesh),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAction, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.retry_download),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun StateDownloading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp), color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.downloading),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.please_wait_while_the_mesh_is_being_downloaded),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StateIdle(onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = stringResource(R.string.download),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.mesh_ready_to_download),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.download_the_3d_mesh_for_this_scan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAction, modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.download_mesh),
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
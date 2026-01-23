package health.openwater.openlifu3dscanner.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ViewCollectionScreen(
    onNavigateBack: () -> Unit,
    onPhotoscanClick: (CollectionItem) -> Unit,
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by collectionViewModel.uiState.collectAsStateWithLifecycle()

    // Helper to convert on-device scans
    fun onDeviceScans(): List<CollectionItem> {
        return getModelsDir().listFiles()
            ?.filter { it.isDirectory }
            ?.map {
                CollectionItem(
                    photoscanId = 0,
                    photocollectionId = 0,
                    name = it.name,
                    creationDate = Date(it.lastModified()),
                    status = null
                )
            } ?: emptyList()
    }

    // Compute combined collectionItems (cloud scans + on-device scans)
    val collectionItems = remember(uiState.photocollections, uiState.photoscans, uiState.hasError) {
        val photocollections = uiState.photocollections
        val photoscans = uiState.photoscans
        val localScans = onDeviceScans()

        if (uiState.isLoading && photoscans == null) return@remember emptyList()

        (photoscans?.reversed()?.map { photoscan ->
            val collection = photocollections?.find { it.id == photoscan.photocollectionId }
            CollectionItem(
                photoscanId = photoscan.id,
                photocollectionId = photoscan.photocollectionId,
                name = collection?.name ?: "",
                creationDate = photoscan.creationDate,
                status = photoscan.status
            )
        } ?: emptyList()).plus(localScans)
            .distinctBy { it.name }
    }

    fun refresh() {
        collectionViewModel.loadPhotocollections()
        collectionViewModel.loadPhotoscans()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.view_collection)) },
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
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { contentPadding ->

        val scroll = rememberScrollState()

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Show offline banner when there's an error but we have local scans
                if (uiState.hasError && collectionItems.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.showing_offline_scans),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                when {
                    !uiState.isLoading && collectionItems.isEmpty() -> {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (uiState.hasError) {
                                    stringResource(R.string.failed_to_load_photo_scans)
                                } else {
                                    stringResource(R.string.no_photo_scans_available)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (uiState.hasError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    else -> {
                        // List of photo scans (cloud + on-device)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(collectionItems) { scan ->
                                PhotoscanCard(
                                    item = scan,
                                    onClick = { onPhotoscanClick(scan) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package health.openwater.openlifu3dscanner.screen.collection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CollectionScreen(
    onNavigateBack: () -> Unit,
    onPhotoscanClick: (CollectionItem) -> Unit,
    collectionViewModel: CollectionViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val uiState by collectionViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val userUiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by userViewModel.isConnected.collectAsStateWithLifecycle()
    val isLoggedIn = userUiState.user != null
    val isOnline = isLoggedIn && isConnected

    // Multi-select state
    val selectedItems = remember { mutableListOf<CollectionItem>().toMutableStateList() }
    val isSelectionMode = selectedItems.isNotEmpty()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteLocal by remember { mutableStateOf(true) }
    var deleteCloud by remember { mutableStateOf(true) }
    var localRefreshKey by remember { mutableStateOf(0) }

    // Helper to convert on-device scans
    fun onDeviceScans(): List<CollectionItem> {
        return getModelsDir().listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
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
    val collectionItems =
        remember(uiState.photocollections, uiState.photoscans, uiState.hasError, localRefreshKey) {
            val photocollections = uiState.photocollections
            val photoscans = uiState.photoscans
            val localScans = onDeviceScans()

            if (uiState.isLoading && photoscans == null) return@remember emptyList()

            (photoscans?.map { photoscan ->
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
                .sortedByDescending { it.creationDate }
        }

    fun refresh() {
        collectionViewModel.loadPhotocollections()
        collectionViewModel.loadPhotoscans()
    }

    fun toggleSelection(item: CollectionItem) {
        if (selectedItems.any { it.name == item.name }) {
            selectedItems.removeAll { it.name == item.name }
        } else {
            selectedItems.add(item)
        }
    }

    // Back handler to exit selection mode
    BackHandler(enabled = isSelectionMode) {
        selectedItems.clear()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    // Loading dialog while deleting
    if (isDeleting) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.deleting),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        val anyHasLocalFiles = remember(selectedItems.toList()) {
            selectedItems.any { item ->
                java.io.File(getModelsDir(), item.name).exists()
            }
        }
        val anyHasCloudData = remember(selectedItems.toList()) {
            selectedItems.any { it.photoscanId != 0L || it.photocollectionId != 0L }
        }

        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_selected_scans, selectedItems.size)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_scan_confirmation))
                    Spacer(modifier = Modifier.height(12.dp))
                    if (anyHasLocalFiles) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteLocal = !deleteLocal }
                        ) {
                            Checkbox(
                                checked = deleteLocal,
                                onCheckedChange = { deleteLocal = it }
                            )
                            Text(
                                text = stringResource(R.string.delete_local_files),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    if (anyHasCloudData) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteCloud = !deleteCloud }
                        ) {
                            Checkbox(
                                checked = deleteCloud,
                                onCheckedChange = { deleteCloud = it }
                            )
                            Text(
                                text = stringResource(R.string.delete_from_cloud),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        isDeleting = true
                        val itemsToDelete = selectedItems.toList()
                        coroutineScope.launch {
                            for (item in itemsToDelete) {
                                collectionViewModel.deleteScan(
                                    photoscanId = item.photoscanId,
                                    photocollectionId = item.photocollectionId,
                                    collectionName = item.name,
                                    deleteLocal = deleteLocal,
                                    deleteCloud = deleteCloud
                                )
                            }
                            selectedItems.clear()
                            isDeleting = false
                            localRefreshKey++
                            refresh()
                        }
                    },
                    enabled = deleteLocal || deleteCloud
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(
                                R.string.n_selected,
                                selectedItems.size
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val anyHasLocal = selectedItems.any { item ->
                                java.io.File(getModelsDir(), item.name).exists()
                            }
                            val anyHasCloud = selectedItems.any {
                                it.photoscanId != 0L || it.photocollectionId != 0L
                            }
                            deleteLocal = anyHasLocal
                            deleteCloud = anyHasCloud
                            showDeleteConfirmation = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete)
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
            } else {
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
                    actions = {
                        Icon(
                            imageVector = if (isOnline) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                            contentDescription = stringResource(
                                if (isOnline) R.string.online else R.string.offline
                            ),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
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
                                text = if (uiState.hasServerError) {
                                    stringResource(R.string.failed_to_load_photo_scans)
                                } else {
                                    stringResource(R.string.no_photo_scans_available)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (uiState.hasServerError) {
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
                                    isSelected = selectedItems.any { it.name == scan.name },
                                    onClick = {
                                        if (isSelectionMode) {
                                            toggleSelection(scan)
                                        } else {
                                            onPhotoscanClick(scan)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            toggleSelection(scan)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

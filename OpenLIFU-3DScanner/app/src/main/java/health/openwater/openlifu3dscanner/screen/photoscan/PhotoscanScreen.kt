package health.openwater.openlifu3dscanner.screen.photoscan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import health.openwater.openlifu3dscanner.repository.ScanConfig
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoscanScreen(
    collectionName: String,
    photoscanId: Long,
    photocollectionId: Long,
    onNavigateBack: () -> Unit,
    onStartProcessing: (() -> Unit)? = null,
    onNavigateToTransfer: (() -> Unit)? = null,
    cloudViewModel: CloudViewModel = hiltViewModel(),
    collectionViewModel: CollectionViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val items = listOf(stringResource(R.string.model), stringResource(R.string.photos))
    var selectedItem by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val hasLocalFiles = remember(collectionName) {
        File(getModelsDir(context), collectionName).exists()
    }
    val hasCloudData = photoscanId != 0L || photocollectionId != 0L
    var deleteLocal by remember { mutableStateOf(true) }
    var deleteCloud by remember { mutableStateOf(true) }

    val userState by userViewModel.uiState.collectAsStateWithLifecycle()
    val hasCredits = (userState.credits ?: 0) > 0
    val isLoggedIn = userState.user != null
    var showNoCreditsWarning by remember { mutableStateOf(false) }

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
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_scan)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_scan_confirmation))
                    Spacer(modifier = Modifier.height(12.dp))
                    if (hasLocalFiles) {
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
                    if (hasCloudData) {
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
                        coroutineScope.launch {
                            collectionViewModel.deleteScan(
                                photoscanId = photoscanId,
                                photocollectionId = photocollectionId,
                                collectionName = collectionName,
                                deleteLocal = deleteLocal,
                                deleteCloud = deleteCloud
                            )
                            isDeleting = false
                            onNavigateBack()
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

    // No credits warning dialog
    if (showNoCreditsWarning) {
        AlertDialog(
            onDismissRequest = { showNoCreditsWarning = false },
            title = { Text(stringResource(R.string.no_credits_title)) },
            text = { Text(stringResource(R.string.no_credits_contact_support)) },
            confirmButton = {
                TextButton(onClick = { showNoCreditsWarning = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    val scanDate = remember(collectionName) {
        val dir = File(getModelsDir(context), collectionName)
        val timestamp = if (dir.exists()) dir.lastModified() else System.currentTimeMillis()
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = scanDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(text = collectionName)
                    }
                }, navigationIcon = {
                    IconButton(onClick = {
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }, actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.actions)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_scan)) },
                                onClick = {
                                    showMenu = false
                                    deleteLocal = hasLocalFiles
                                    deleteCloud = hasCloudData
                                    showDeleteConfirmation = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.ViewInAr else Icons.Default.Photo,
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { contentPadding ->

        Box(modifier = Modifier.padding(contentPadding)) {
            when (selectedItem) {
                0 -> ModelTab(
                    collectionName = collectionName,
                    photoscanId = photoscanId,
                    photocollectionId = photocollectionId,
                    isLoggedIn = isLoggedIn,
                    onTransferToPc = onNavigateToTransfer?.let { navigate ->
                        {
                            cloudViewModel.setScanConfig(
                                ScanConfig(
                                    collectionName = collectionName,
                                    autoUploadEnabled = false,
                                    sessionId = null
                                )
                            )
                            navigate()
                        }
                    },
                    onStartProcessing = if (onStartProcessing != null) {
                        {
                            if (hasCredits) {
                                cloudViewModel.setScanConfig(
                                    ScanConfig(
                                        collectionName = collectionName,
                                        autoUploadEnabled = false,
                                        sessionId = null
                                    )
                                )
                                onStartProcessing()
                            } else {
                                showNoCreditsWarning = true
                            }
                        }
                    } else null
                )
                1 -> PhotosTab(collectionName, photoscanId, photocollectionId)
            }
        }
    }
}
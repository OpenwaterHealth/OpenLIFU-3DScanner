package health.openwater.openlifu3dscanner.screen.photoscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import health.openwater.openlifu3dscanner.viewmodel.CollectionViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoscanScreen(
    collectionName: String,
    photoscanId: Long,
    photocollectionId: Long,
    onNavigateBack: () -> Unit,
    onStartProcessing: (() -> Unit)? = null,
    collectionViewModel: CollectionViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val items = listOf(stringResource(R.string.model), stringResource(R.string.photos))
    var selectedItem by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
            text = { Text(stringResource(R.string.delete_scan_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        isDeleting = true
                        coroutineScope.launch {
                            collectionViewModel.deleteScan(
                                photoscanId = photoscanId,
                                photocollectionId = photocollectionId,
                                collectionName = collectionName
                            )
                            isDeleting = false
                            onNavigateBack()
                        }
                    }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = collectionName)
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
                    onStartProcessing = if (onStartProcessing != null) {
                        {
                            if (hasCredits) {
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
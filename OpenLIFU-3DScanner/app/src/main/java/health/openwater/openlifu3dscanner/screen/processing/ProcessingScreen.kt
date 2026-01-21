package health.openwater.openlifu3dscanner.screen.processing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    collectionName: String,
    onNavigateBack: () -> Unit,
    onNavigateToUploading: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    val userInfo by userViewModel.getUserInfo().collectAsState(initial = null)
    val isLoggedIn = userInfo != null

    fun onBack() {
        if (isLoggedIn) {
            showCancelDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = collectionName) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
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
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            ProcessingRoot(
                collectionName = collectionName,
                onUpload = onNavigateToUploading,
                onTransferToPc = onNavigateToTransfer
            )
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(text = stringResource(R.string.cancel_processing)) },
            text = {
                Text(
                    text = stringResource(R.string.are_you_sure_you_want_to_cancel_processing_all_unsaved_progress_will_be_lost)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
package health.openwater.openlifu3dscanner.screen.scanner

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: (autoUploadEnabled: Boolean, isLoggedIn: Boolean) -> Unit,
    cloudViewModel: CloudViewModel = hiltViewModel()
) {
    val scanConfig = cloudViewModel.scanConfig
    if (scanConfig == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val collectionName = scanConfig.collectionName
    val autoUploadEnabled = scanConfig.autoUploadEnabled
    val sessionId = scanConfig.sessionId

    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val hasCredits = (uiState.credits ?: 0) > 0
    val isLoggedIn = uiState.user != null && hasCredits

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        cloudViewModel.reset(false)

        if (cloudViewModel.isLoggedInAndOnline() && hasCredits) {
            cloudViewModel.createPhotocollection(collectionName, autoUploadEnabled, sessionId)
        }
    }

    BackHandler {
        cloudViewModel.reset(false)
        onNavigateBack()
    }

    KeepScreenOn()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.scanning)) },
                navigationIcon = {
                    IconButton(onClick = {
                        cloudViewModel.reset(false)
                        onNavigateBack()
                    }) {
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
                .padding(contentPadding),
        ) {
            ScannerComponent(
                collectionName = collectionName,
                autoUploadEnabled = autoUploadEnabled,
                snackbarHostState = snackbarHostState,
                onProceed = {
                    onNavigateToProcessing(autoUploadEnabled, isLoggedIn)
                }
            )
        }
    }
}

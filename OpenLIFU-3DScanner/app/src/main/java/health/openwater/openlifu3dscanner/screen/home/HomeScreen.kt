package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.BuildConfig
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.utils.CollectionIdGenerator
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@Composable
fun HomeScreen(
    onStartScan: (collectionName: String, autoUploadEnabled: Boolean) -> Unit,
    onViewCollection: () -> Unit,
    onSettings: () -> Unit,
    onSignIn: () -> Unit,
) {
    var showCollectionDialog by remember { mutableStateOf(false) }
    var collectionName by remember { mutableStateOf("") }
    var autoUploadEnabled by remember { mutableStateOf(true) }

    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.openwater),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeRoot(
                onStartScan = {
                    showCollectionDialog = true
                    collectionName = CollectionIdGenerator.generate()
                },
                onSettings = onSettings,
                onViewCollection = onViewCollection,
                onSignIn = onSignIn
            )
        }
    }

    if (showCollectionDialog) {

        AlertDialog(
            onDismissRequest = {
                showCollectionDialog = false
            },
            title = {
                Text(text = stringResource(R.string.enter_collection_id))
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = collectionName,
                        onValueChange = { collectionName = it },
                        label = { Text(stringResource(R.string.collection_id)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.user != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { autoUploadEnabled = !autoUploadEnabled }
                        ) {
                            Checkbox(
                                checked = autoUploadEnabled,
                                onCheckedChange = { autoUploadEnabled = it }
                            )
                            Text(
                                text = "Auto Upload",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = collectionName.isNotBlank(),
                    onClick = {
                        showCollectionDialog = false
                        onStartScan(
                            collectionName.trim(),
                            autoUploadEnabled
                        )
                        collectionName = ""
                        autoUploadEnabled = true
                    }
                ) {
                    Text(stringResource(R.string.start_scan))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCollectionDialog = false
                        collectionName = ""
                        autoUploadEnabled = true
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )

    }
}
package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    userViewModel: UserViewModel = hiltViewModel()
) {
    var showCollectionDialog by remember { mutableStateOf(false) }
    var collectionName by remember { mutableStateOf("") }
    var autoUploadEnabled by remember { mutableStateOf(true) }

    val userInfo by userViewModel.getUserInfo().collectAsState(initial = null)
    val isLoggedIn = userInfo != null

    Scaffold(
        bottomBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.openwater),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .offset(y = (-8).dp)
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

                    if (isLoggedIn) {
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
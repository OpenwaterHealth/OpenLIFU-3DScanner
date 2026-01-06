package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import health.openwater.openlifu3dscanner.BuildConfig
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.utils.CollectionIdGenerator

@Composable
fun HomeScreen(
    onStartScan: (String) -> Unit,
    onViewCollection: () -> Unit
) {
    var showCollectionDialog by remember { mutableStateOf(false) }
    var collectionName by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .fillMaxWidth()
                    .padding(bottom = 34.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.openwater),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.72f)
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .alpha(0.72f)
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
                onViewCollection = onViewCollection
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
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = { collectionName = it },
                    label = { Text(stringResource(R.string.collection_id)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = collectionName.isNotBlank(),
                    onClick = {
                        showCollectionDialog = false
                        onStartScan(collectionName.trim())
                        collectionName = ""
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
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
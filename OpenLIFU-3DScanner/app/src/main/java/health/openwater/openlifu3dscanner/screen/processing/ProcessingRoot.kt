package health.openwater.openlifu3dscanner.screen.processing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import java.io.File


@Composable
fun ProcessingRoot(
    collectionName: String,
    onUpload: () -> Unit,
    onTransferToPc: () -> Unit,
) {
    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by userViewModel.isConnected.collectAsStateWithLifecycle()
    val hasCredits = (uiState.credits ?: 0) > 0
    val canUpload = uiState.user != null && hasCredits && isConnected

    val scanDir =
        remember(collectionName) { File(getModelsDir(), collectionName) }

    var imageFiles by remember(scanDir) {
        mutableStateOf(
            scanDir
                .listFiles { f -> f.extension.equals("jpg", true) }
                ?.sortedBy { it.name }
                ?: emptyList()
        )
    }

    var selectedImage by remember {
        mutableStateOf(imageFiles.firstOrNull())
    }

    var imageToDelete by remember { mutableStateOf<File?>(null) }

    // Confirmation dialog
    imageToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text(stringResource(R.string.delete_image)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_to_delete_photo)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (file.delete()) {
                            val currentIndex = imageFiles.indexOf(file)
                            imageFiles = imageFiles.filter { it != file }

                            // Update selected image if needed
                            if (selectedImage == file) {
                                selectedImage = when {
                                    imageFiles.isEmpty() -> null
                                    currentIndex < imageFiles.size -> imageFiles[currentIndex]
                                    else -> imageFiles.lastOrNull()
                                }
                            }
                            val metadataFile = File(scanDir, file.name.replace(".jpg", ".json"))
                            metadataFile.delete()
                        }
                        imageToDelete = null
                    }
                ) {
                    Text(text = stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with image count
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.photos_captured),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.photo_count, imageFiles.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Main image viewer with shadow and rounded corners
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A1A)),
                    contentAlignment = Alignment.Center
                ) {
                    selectedImage?.let { file ->
                        Image(
                            painter = rememberAsyncImagePainter(file),
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        Text(
                            text = stringResource(R.string.no_image_selected),
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Thumbnail row with improved styling
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(imageFiles) { image ->
                        ThumbnailItemWithDelete(
                            file = image,
                            selected = image == selectedImage,
                            onClick = { selectedImage = image },
                            onDelete = { imageToDelete = image }
                        )
                    }
                }
            }

            // Upload button
            Button(
                onClick = {
                    if (canUpload) {
                        onUpload()
                    } else {
                        onTransferToPc()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = imageFiles.isNotEmpty()
            ) {

                Icon(
                    imageVector = if (canUpload) Icons.Default.Upload else Icons.Default.Usb,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = stringResource(if (canUpload) R.string.start_processing else R.string.transfer_to_pc),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ThumbnailItemWithDelete(
    file: File,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .shadow(
                elevation = if (selected) 8.dp else 2.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(file),
            contentDescription = file.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Delete button overlay
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
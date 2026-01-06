package health.openwater.openlifu3dscanner.screen.processing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import java.io.File


@Composable
fun ProcessingRoot(
    collectionName: String,
    onUpload: () -> Unit
) {
    val context = LocalContext.current
    val scanDir =
        remember(collectionName) { File(context.getModelsDir(), collectionName) }

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

    Column(modifier = Modifier.fillMaxSize()) {

        // Horizontal previews
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(imageFiles) { image ->
                PreviewItemWithDelete(
                    file = image,
                    selected = image == selectedImage,
                    onClick = { selectedImage = image },
                    onDelete = { imageToDelete = image }
                )
            }
        }

        // Main image viewer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            selectedImage?.let { file ->
                Image(
                    painter = rememberAsyncImagePainter(file),
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Upload button
        Surface(shadowElevation = 8.dp) {
            Button(
                onClick = {
                    onUpload()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = imageFiles.isNotEmpty()
            ) {
                Text(
                    text = stringResource(R.string.upload),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewItemWithDelete(
    file: File,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box {
        PreviewItem(
            file = file,
            selected = selected,
            onClick = onClick
        )

        // Delete button overlay
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
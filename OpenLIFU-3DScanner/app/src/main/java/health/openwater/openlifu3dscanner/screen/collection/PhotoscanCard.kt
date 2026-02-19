package health.openwater.openlifu3dscanner.screen.collection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.extensions.hasLocalModel
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CollectionItem(
    val photoscanId: Long,
    val photocollectionId: Long,
    val name: String,
    val creationDate: Date,
    val status: PhotoscanStatus?
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoscanCard(
    item: CollectionItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    val collectionDir = remember(item.name) { File(getModelsDir(), item.name) }

    val imageFiles = remember(item.name) {
        collectionDir.listFiles { file ->
            file.extension.equals("jpg", ignoreCase = true) ||
                    file.extension.equals("jpeg", ignoreCase = true)
        } ?: emptyArray()
    }

    val firstImage = remember(item.name) { imageFiles.minByOrNull { it.name } }
    val photoCount = imageFiles.size

    val hasModel = remember(item.name) { hasLocalModel(item.name) }

    val shape = RoundedCornerShape(12.dp)
    val borderModifier = if (isSelected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .then(borderModifier)
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            if (firstImage != null) {
                AsyncImage(
                    model = firstImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Selection checkmark top right
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(28.dp)
                )
            }

            // Status icons top left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.status != null) {
                    Icon(
                        imageVector = Icons.Filled.Cloud,
                        contentDescription = stringResource(R.string.cloud_available),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (hasModel) {
                    Icon(
                        imageVector = Icons.Filled.ViewInAr,
                        contentDescription = stringResource(R.string.model_available),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Content
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDate(item.creationDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        if (photoCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "$photoCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                if (item.status != null) {
                    StatusChip(status = item.status)
                }
            }
        }
    }
}


private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

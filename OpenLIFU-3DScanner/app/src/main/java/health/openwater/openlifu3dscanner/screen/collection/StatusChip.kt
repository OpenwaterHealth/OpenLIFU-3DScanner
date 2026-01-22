package health.openwater.openlifu3dscanner.screen.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus

@Composable
fun StatusChip(status: PhotoscanStatus?) {
    val (icon, color, text) = when (status) {
        PhotoscanStatus.STARTED -> Triple(
            Icons.Default.HourglassEmpty,
            Color(0xFFFFA726),
            stringResource(R.string.started)
        )
        PhotoscanStatus.RUNNING -> Triple(
            Icons.Default.PlayArrow,
            Color(0xFF42A5F5),
            stringResource(R.string.running)
        )
        PhotoscanStatus.FINISHED -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF66BB6A),
            stringResource(R.string.finished)
        )
        PhotoscanStatus.FAILED -> Triple(
            Icons.Default.Error,
            Color(0xFFEF5350),
            stringResource(R.string.failed)
        )
        PhotoscanStatus.STOPPED -> Triple(
            Icons.Default.Stop,
            Color(0xFF9E9E9E),
            stringResource(R.string.stopped)
        )
        null -> Triple(
            Icons.Default.HourglassEmpty,
            Color(0xFF9E9E9E),
            stringResource(R.string.unknown)
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
package health.openwater.openlifu3dscanner.screen.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RecordButton(
    enabled: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .clickable { if (enabled) onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = 4.dp,
                    color = Color.White,
                    shape = CircleShape
                )
        )

        // Inner shape
        Box(
            modifier = Modifier
                .size(if (isRecording) 32.dp else 48.dp)
                .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                .background(if (enabled) Color.Red else Color.Gray)
        )
    }
}

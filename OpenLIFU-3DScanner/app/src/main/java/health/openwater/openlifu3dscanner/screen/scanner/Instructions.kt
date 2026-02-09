package health.openwater.openlifu3dscanner.screen.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.openwater.openlifu3dscanner.R

@Composable
fun Instructions() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string._1_point_camera_at_face),
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string._2_tap_button),
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string._3_walk_in_a_circle_around_person),
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.photos_capture_automatically),
            color = Color.Yellow,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
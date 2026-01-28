package health.openwater.openlifu3dscanner.screen.scanner

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.drawSegmentedArc
import health.openwater.openlifu3dscanner.viewmodel.ScannerViewModel

@Composable
fun ScanControls(
    collectionName: String,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent black overlay with oval cutout
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw full black background
            drawRect(color = Color.Black.copy(alpha = 0.7f))

            // Calculate oval dimensions (centered, portrait-oriented)
            val ovalWidth = size.width * 0.65f
            val ovalHeight = size.height * 0.45f
            val ovalLeft = (size.width - ovalWidth) / 2f
            val ovalTop = (size.height - ovalHeight) / 2f - size.height * 0.2f

            // Cut out the oval using BlendMode
            drawOval(
                color = Color.Transparent,
                topLeft = Offset(ovalLeft, ovalTop),
                size = Size(ovalWidth, ovalHeight),
                blendMode = BlendMode.Clear
            )
        }

        // UI Elements on top
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(
                            R.string.photos_d_d,
                            viewModel.capturedBuckets.size,
                            viewModel.totalBuckets
                        ),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isScanning) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(
                                R.string.face_s,
                                if (viewModel.faceDetected) "☑" else "☐"
                            ),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                if (!isScanning) {
                    if (!isCompleted) {
                        Instructions()
                    }

                } else {
                    if (!isCompleted) {
                        Text(
                            text = stringResource(R.string.walk_to_next_capture_point),
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress percentage based on captures
                    val progress =
                        (viewModel.capturedBuckets.size.toFloat() / viewModel.totalBuckets.toFloat() * 100).toInt()
                    Text(
                        text = stringResource(R.string.complete, progress),
                        color = Color.Cyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (viewModel.capturedBuckets.size < viewModel.totalBuckets) {

                    Spacer(modifier = Modifier.height(24.dp))
                    RecordButton(
                        enabled = viewModel.faceDetected || isScanning,
                        isRecording = isScanning,
                        onClick = {
                                if (!isScanning) {
                                    viewModel.startScanning(collectionName)
                                } else {
                                    viewModel.stopScanning()
                                }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "blink")
        val blinkAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blinkAlpha"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isScanning) return@Canvas

            val centerX = size.width / 2f
            val centerY = size.height / 2f - size.height * 0.2f

            val ovalRadiusX = size.width * 0.65f / 2f + 8f  // Slightly outside the cutout
            val ovalRadiusY = size.height * 0.45f / 2f + 8f
            val ovalCenter = Offset(centerX, centerY)

            val totalSegments = (360f / viewModel.captureInterval).toInt()
            val segmentSweep = viewModel.captureInterval - 2f  // Gap between segments
            val strokeWidth = 8f

            // Get relative angle from starting position
            val relativeAngle = viewModel.getRelativeAngle()

            // Draw segmented arc border
            for (i in 0 until totalSegments) {
                val startAngle = i * viewModel.captureInterval - 90f  // Start from top
                val isCaptured = viewModel.capturedBuckets.contains(i)
                val isCurrent = (relativeAngle / viewModel.captureInterval).toInt() == i

                val segmentColor = when {
                    isCurrent -> Color.Cyan
                    isCaptured -> Color(0xFF00E676)  // Bright green
                    else -> Color.White.copy(alpha = 0.25f)
                }

                drawSegmentedArc(
                    center = ovalCenter,
                    radiusX = ovalRadiusX,
                    radiusY = ovalRadiusY,
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    color = segmentColor,
                    strokeWidth = strokeWidth,
                    isCaptured = isCaptured,
                    isCurrent = isCurrent,
                    blinkAlpha = if (isCurrent) blinkAlpha else 1f
                )
            }
        }
    }
}
package health.openwater.openlifu3dscanner.screen.scanner

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.extensions.drawSegmentedArc
import health.openwater.openlifu3dscanner.preferences.Prefs
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import health.openwater.openlifu3dscanner.viewmodel.FaceStatus
import health.openwater.openlifu3dscanner.viewmodel.ScannerViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

private const val MIN_IMAGES_COUNT = 20
private const val OVAL_ASPECT_RATIO = 1.3f        // height / width – fixed regardless of screen width
private const val MAX_OVAL_HEIGHT_OF_WIDTH = 1.2f   // safety cap; 0.90 * 1.3 = 1.17, so normal phones are never capped

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ScanControls(
    collectionName: String,
    autoUploadEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    onProceed: () -> Unit,
    cloudViewModel: CloudViewModel = hiltViewModel(),
    scannerViewModel: ScannerViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val isScanning by scannerViewModel.isScanning.collectAsState()
    val isCompleted by scannerViewModel.isCompleted.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by userViewModel.isConnected.collectAsStateWithLifecycle()
    val hasCredits = (uiState.credits ?: 0) > 0
    val isLoggedIn = uiState.user != null
    val isOnline = isLoggedIn && hasCredits && isConnected

    val ovalSizePref = remember { Prefs.getOvalSize(context, isOnline) }
    val ovalWidthFactor = when (ovalSizePref) {
        150 -> 0.78f
        200 -> 0.90f
        else -> 0.65f
    }
    val ovalTop = 100f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        LaunchedEffect(ovalWidthFactor, screenWidthPx, screenHeightPx) {
            if (screenWidthPx > 0f && screenHeightPx > 0f) {
                val ovalHeight = minOf(screenWidthPx * ovalWidthFactor * OVAL_ASPECT_RATIO, screenWidthPx * MAX_OVAL_HEIGHT_OF_WIDTH)
                val ovalWidth = ovalHeight / OVAL_ASPECT_RATIO
                scannerViewModel.setOvalBounds(
                    widthFactor = ovalWidth / screenWidthPx,
                    heightFactor = ovalHeight / screenHeightPx,
                    topFraction = ovalTop / screenHeightPx
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Semi-transparent black overlay with oval cutout
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw full black background
                drawRect(color = Color.Black.copy(alpha = 0.3f))

                // Calculate oval dimensions (centered, portrait-oriented, fixed aspect ratio, capped height)
                val ovalHeight = minOf(size.width * ovalWidthFactor * OVAL_ASPECT_RATIO, size.width * MAX_OVAL_HEIGHT_OF_WIDTH)
                val ovalWidth = ovalHeight / OVAL_ASPECT_RATIO
                val ovalLeft = (size.width - ovalWidth) / 2f

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
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.photos_d_d,
                            scannerViewModel.capturedBuckets.size,
                            scannerViewModel.totalBuckets
                        ),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!isScanning) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = when (scannerViewModel.faceStatus) {
                                    FaceStatus.READY -> stringResource(R.string.face_ready)
                                    FaceStatus.CENTER_FACE -> stringResource(R.string.center_face_in_oval)
                                    FaceStatus.MOVE_CLOSER -> stringResource(R.string.move_closer)
                                    FaceStatus.NO_FACE -> stringResource(R.string.no_face_found)
                                },
                                color = if (scannerViewModel.faceStatus == FaceStatus.READY) Color(
                                    0xFF00E676
                                )
                                else Color(0xFFFF9800),
                                fontSize = 18.sp,
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
                            if (!scannerViewModel.isOrientationValid) {
                                Text(
                                    text = stringResource(R.string.hold_phone_upright),
                                    color = Color(0xFFFF9800),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.walk_to_next_capture_point),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress percentage based on captures
                        val progress =
                            (scannerViewModel.capturedBuckets.size.toFloat() / scannerViewModel.totalBuckets.toFloat() * 100).toInt()
                        Text(
                            text = stringResource(R.string.complete, progress),
                            color = Color.Cyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isScanning) {
                        // Stop button - full width
                        Button(
                            onClick = { scannerViewModel.stopScanning() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935)  // Red
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.stop_scan),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (scannerViewModel.capturedBuckets.isNotEmpty()) {
                        // Re-capture and Proceed buttons - 50/50 width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Reset photocollection if auto-upload was enabled
                                    if (autoUploadEnabled) {
                                        cloudViewModel.resetPhotocollection(
                                            collectionName,
                                            autoUploadEnabled
                                        )
                                    }
                                    scannerViewModel.resetForRecapture(collectionName)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.recapture),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    if (scannerViewModel.capturedBuckets.size < MIN_IMAGES_COUNT) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = context.getString(
                                                    R.string.you_need_to_capture_at_least_d_images_to_proceed,
                                                    MIN_IMAGES_COUNT
                                                )
                                            )
                                        }
                                    } else {
                                        cloudViewModel.onScanComplete()
                                        onProceed()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E676)  // Green
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.proceed),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    } else {
                        // Start button - full width
                        Button(
                            onClick = { scannerViewModel.startScanning(collectionName) },
                            enabled = scannerViewModel.faceDetected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.start_scan),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Sticky level: lock to horizontal when level, only unlock beyond breakout threshold
            var isLockedLevel by remember { mutableStateOf(false) }
            val roll = scannerViewModel.currentRoll
            val absRoll = abs(roll)
            if (!isLockedLevel && absRoll < 3f) isLockedLevel = true
            if (isLockedLevel && absRoll > 7f) isLockedLevel = false

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
                val ovalHeight = minOf(size.width * ovalWidthFactor * OVAL_ASPECT_RATIO, size.width * MAX_OVAL_HEIGHT_OF_WIDTH)
                val ovalWidth = ovalHeight / OVAL_ASPECT_RATIO
                val centerX = size.width / 2f
                val centerY = ovalHeight / 2f + ovalTop

                val ovalRadiusX = ovalWidth / 2f + 8f  // Slightly outside the cutout
                val ovalRadiusY = ovalHeight / 2f + 8f
                val ovalCenter = Offset(centerX, centerY)

                // Static horizontal reference ticks outside the tilt indicator
                val lineHalfLength = ovalRadiusX * 0.35f
                val gap = 8f
                val tickLength = lineHalfLength * 0.25f
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(centerX - lineHalfLength - gap - tickLength, centerY),
                    end = Offset(centerX - lineHalfLength - gap, centerY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(centerX + lineHalfLength + gap, centerY),
                    end = Offset(centerX + lineHalfLength + gap + tickLength, centerY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // Tilt level indicator line
                val displayRoll = if (isLockedLevel) 0f else roll
                val levelColor = if (isLockedLevel) {
                    Color(0xFF00E676)
                } else when {
                    absRoll < 10f -> Color(0xFFFFEB3B)  // Yellow — slight tilt
                    absRoll < 20f -> Color(0xFFFF9800)  // Orange — moderate tilt
                    else -> Color(0xFFE53935)           // Red — heavy tilt
                }
                rotate(degrees = -displayRoll, pivot = ovalCenter) {
                    drawLine(
                        color = levelColor,
                        start = Offset(centerX - lineHalfLength, centerY),
                        end = Offset(centerX + lineHalfLength, centerY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                val totalSegments = (360f / scannerViewModel.captureInterval).toInt()
                val segmentSweep = scannerViewModel.captureInterval - 2f  // Gap between segments
                val strokeWidth = 12f

                // Get relative angle from starting position
                val relativeAngle = scannerViewModel.getRelativeAngle()

                // Draw segmented arc border
                for (i in 0 until totalSegments) {
                    val startAngle = i * scannerViewModel.captureInterval - 90f  // Start from top
                    val isCaptured = scannerViewModel.capturedBuckets.contains(i)
                    val isCurrent =
                        isScanning && (relativeAngle / scannerViewModel.captureInterval).toInt() == i

                    val segmentColor = when {
                        isCurrent -> Color.Yellow
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
}

// region Previews

private fun previewScanControlsContent(
    isScanning: Boolean,
    isCompleted: Boolean,
    capturedCount: Int,
    totalBuckets: Int,
    faceStatusLabel: String,
    faceStatusColor: Color,
    showFaceStatus: Boolean,
    progress: Int,
    showOrientationWarning: Boolean,
    startEnabled: Boolean
): @Composable () -> Unit = {
    val ovalWidthFactor = 0.65f
    val ovalTop = 100f
    val captureInterval = 360f / totalBuckets

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.3f))
            val ovalHeight = minOf(size.width * ovalWidthFactor * OVAL_ASPECT_RATIO, size.width * MAX_OVAL_HEIGHT_OF_WIDTH)
            val ovalWidth = ovalHeight / OVAL_ASPECT_RATIO
            val ovalLeft = (size.width - ovalWidth) / 2f
            drawOval(
                color = Color.Transparent,
                topLeft = Offset(ovalLeft, ovalTop),
                size = Size(ovalWidth, ovalHeight),
                blendMode = BlendMode.Clear
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photos $capturedCount / $totalBuckets",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (showFaceStatus) {
                    Text(
                        text = faceStatusLabel,
                        color = faceStatusColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (!isScanning) {
                    if (!isCompleted) Instructions()
                } else {
                    if (!isCompleted) {
                        if (showOrientationWarning) {
                            Text(
                                text = "Hold phone upright",
                                color = Color(0xFFFF9800),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Walk to next capture point",
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$progress% Complete",
                        color = Color.Cyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                when {
                    isScanning -> Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text(text = "Stop Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    capturedCount > 0 -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(text = "Re-Capture", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text(text = "Proceed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    else -> Button(
                        onClick = {},
                        enabled = startEnabled,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Start Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val ovalHeight = minOf(size.width * ovalWidthFactor * OVAL_ASPECT_RATIO, size.width * MAX_OVAL_HEIGHT_OF_WIDTH)
            val ovalWidth = ovalHeight / OVAL_ASPECT_RATIO
            val centerX = size.width / 2f
            val centerY = ovalHeight / 2f + ovalTop
            val ovalRadiusX = ovalWidth / 2f + 8f
            val ovalRadiusY = ovalHeight / 2f + 8f
            val ovalCenter = Offset(centerX, centerY)
            val lineHalfLength = ovalRadiusX * 0.35f
            val gap = 8f
            val tickLength = lineHalfLength * 0.25f

            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX - lineHalfLength - gap - tickLength, centerY),
                end = Offset(centerX - lineHalfLength - gap, centerY),
                strokeWidth = 2f, cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(centerX + lineHalfLength + gap, centerY),
                end = Offset(centerX + lineHalfLength + gap + tickLength, centerY),
                strokeWidth = 2f, cap = StrokeCap.Round
            )
            rotate(degrees = 0f, pivot = ovalCenter) {
                drawLine(
                    color = Color(0xFF00E676),
                    start = Offset(centerX - lineHalfLength, centerY),
                    end = Offset(centerX + lineHalfLength, centerY),
                    strokeWidth = 3f, cap = StrokeCap.Round
                )
            }

            for (i in 0 until totalBuckets) {
                val startAngle = i * captureInterval - 90f
                val isCaptured = i < capturedCount
                val segmentColor = when {
                    isCaptured -> Color(0xFF00E676)
                    else -> Color.White.copy(alpha = 0.25f)
                }
                drawSegmentedArc(
                    center = ovalCenter,
                    radiusX = ovalRadiusX,
                    radiusY = ovalRadiusY,
                    startAngle = startAngle,
                    sweepAngle = captureInterval - 2f,
                    color = segmentColor,
                    strokeWidth = 12f,
                    isCaptured = isCaptured,
                    isCurrent = false,
                    blinkAlpha = 1f
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 590, heightDp = 844, name = "Idle – no face")
@Composable
private fun ScanControlsIdlePreview() {
    previewScanControlsContent(
        isScanning = false, isCompleted = false,
        capturedCount = 0, totalBuckets = 36,
        faceStatusLabel = "No Face Found", faceStatusColor = Color(0xFFFF9800),
        showFaceStatus = true, progress = 0,
        showOrientationWarning = false, startEnabled = false
    ).invoke()
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Scanning – 10 of 36")
@Composable
private fun ScanControlsScanningPreview() {
    previewScanControlsContent(
        isScanning = true, isCompleted = false,
        capturedCount = 10, totalBuckets = 36,
        faceStatusLabel = "", faceStatusColor = Color.White,
        showFaceStatus = false, progress = 28,
        showOrientationWarning = false, startEnabled = false
    ).invoke()
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Post-scan – recapture / proceed")
@Composable
private fun ScanControlsPostScanPreview() {
    previewScanControlsContent(
        isScanning = false, isCompleted = false,
        capturedCount = 36, totalBuckets = 36,
        faceStatusLabel = "Face Ready", faceStatusColor = Color(0xFF00E676),
        showFaceStatus = true, progress = 100,
        showOrientationWarning = false, startEnabled = false
    ).invoke()
}

// endregion
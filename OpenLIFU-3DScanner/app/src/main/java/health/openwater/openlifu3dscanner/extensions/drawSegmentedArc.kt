package health.openwater.openlifu3dscanner.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawSegmentedArc(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    strokeWidth: Float,
    isCaptured: Boolean,
    isCurrent: Boolean,
    blinkAlpha: Float
) {
    // Apply blinking alpha to current segment
    val finalColor = when {
        isCurrent -> color.copy(alpha = color.alpha * blinkAlpha)
        else -> color
    }

    val topLeft = Offset(center.x - radiusX, center.y - radiusY)
    val size = Size(radiusX * 2, radiusY * 2)

    // Draw glow effect for captured or current segments
    if (isCaptured || isCurrent) {
        // Outer glow
        drawArc(
            color = finalColor.copy(alpha = 0.3f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = strokeWidth + 8f, cap = StrokeCap.Round)
        )
        // Middle glow
        drawArc(
            color = finalColor.copy(alpha = 0.5f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = strokeWidth + 4f, cap = StrokeCap.Round)
        )
    }

    // Main arc segment
    drawArc(
        color = finalColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

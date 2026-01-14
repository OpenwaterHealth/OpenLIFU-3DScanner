package health.openwater.openlifu3dscanner.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun DrawScope.drawPetal(
    center: Offset,
    angle: Float,
    radiusX: Float,
    radiusY: Float,
    color: Color,
    isCaptured: Boolean,
    isCurrent: Boolean,
    petalWidth: Float,
    blinkAlpha: Float
) {
    val petalLength = if (isCurrent) petalWidth * 2 else petalWidth
    val filled = isCaptured || isCurrent

    val rad = Math.toRadians(angle.toDouble() - 90)
    val cosAngle = cos(rad).toFloat()
    val sinAngle = sin(rad).toFloat()

    // Calculate point on oval perimeter
    val ovalX = center.x + radiusX * cosAngle
    val ovalY = center.y + radiusY * sinAngle

    // Calculate the normal to the ellipse at this point
    val normalX = (radiusX * cosAngle) / (radiusX * radiusX)
    val normalY = (radiusY * sinAngle) / (radiusY * radiusY)
    val normalMag = sqrt(normalX * normalX + normalY * normalY)
    val normX = normalX / normalMag
    val normY = normalY / normalMag

    // The tangent is perpendicular to the normal
    val tangentX = -normY
    val tangentY = normX

    // Base of petal (on oval perimeter)
    val baseX = ovalX
    val baseY = ovalY

    // Tip of petal (extending outward along the normal)
    val tipX = baseX + normX * petalLength
    val tipY = baseY + normY * petalLength

    val path = Path().apply {
        moveTo(
            baseX + tangentX * petalWidth / 2,
            baseY + tangentY * petalWidth / 2
        )

        quadraticBezierTo(
            tipX, tipY,
            baseX - tangentX * petalWidth / 2,
            baseY - tangentY * petalWidth / 2
        )

        close()
    }

    // Apply blinking alpha to current petal
    val finalColor = if (isCurrent) {
        color.copy(alpha = color.alpha * blinkAlpha)
    } else {
        color
    }

    if (filled) {
        drawPath(path, finalColor, style = Fill)
    } else {
        drawPath(path, finalColor, style = Stroke(width = 2f))
    }
}
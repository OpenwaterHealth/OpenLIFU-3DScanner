package health.openwater.openlifu3dscanner.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawPetal(
    center: Offset,
    angle: Float,
    radiusX: Float,
    radiusY: Float,
    color: Color,
    isCaptured: Boolean,
    isCurrent: Boolean,
) {
    val petalLength = if (isCurrent) 180f else 120f
    val petalWidth = 20f
    val filled = isCaptured || isCurrent

    val rad = Math.toRadians(angle.toDouble() - 90)

    // Calculate point on oval perimeter
    val ovalX = center.x + radiusX * cos(rad).toFloat()
    val ovalY = center.y + radiusY * sin(rad).toFloat()

    // Calculate distance from center to oval point
    val distToOval = kotlin.math.sqrt(
        ((ovalX - center.x) * (ovalX - center.x) +
                (ovalY - center.y) * (ovalY - center.y))
    )

    // Normalized direction vector
    val dirX = (ovalX - center.x) / distToOval
    val dirY = (ovalY - center.y) / distToOval

    // Base of petal (on oval perimeter)
    val baseX = ovalX
    val baseY = ovalY

    // Tip of petal (extending outward)
    val tipX = baseX + dirX * petalLength
    val tipY = baseY + dirY * petalLength

    // Perpendicular direction for petal width
    val perpX = -dirY
    val perpY = dirX

    val path = Path().apply {
        moveTo(
            baseX + perpX * petalWidth / 2,
            baseY + perpY * petalWidth / 2
        )

        quadraticBezierTo(
            tipX, tipY,
            baseX - perpX * petalWidth / 2,
            baseY - perpY * petalWidth / 2
        )

        close()
    }

    if (filled) {
        drawPath(path, color, style = Fill)
    } else {
        drawPath(path, color, style = Stroke(width = 2f))
    }
}
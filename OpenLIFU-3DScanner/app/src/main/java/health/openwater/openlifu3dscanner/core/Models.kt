package health.openwater.openlifu3dscanner.core

data class FaceInfo(
    val centerX: Float,
    val centerY: Float,
    val width: Int,
    val height: Int
)

data class CaptureData(
    val timestamp: Long,
    val angle: Float,              // Relative angle from start (0-360)
    val absoluteAngle: Float,      // Absolute compass angle
    val captureIndex: Int,
    val filename: String,
    val forwardX: Float,
    val forwardY: Float,
    val forwardZ: Float
)

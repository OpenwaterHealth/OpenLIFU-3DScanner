package health.openwater.openlifu3dscanner.Modals

data class ArcConfig(
    val type: String,
    val radius: Double,
    val bulletCount: Int,
    val upDown: Double,
    val closeFar: Double,
    val minAngle: Int,
    val maxAngle: Int
)

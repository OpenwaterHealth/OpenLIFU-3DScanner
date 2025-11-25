package health.openwater.openlifu3dscanner.api.dto

data class ImageCoordinates(
    val image: String,
    val x: Float,
    val y: Float,
    val z: Float
)

data class Coordinates(
    val images: List<ImageCoordinates>
)
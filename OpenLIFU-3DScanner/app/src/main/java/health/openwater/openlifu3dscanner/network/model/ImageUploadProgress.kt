package health.openwater.openlifu3dscanner.network.model

data class ImageUploadProgress(
    val progress: Int,
    val uploadedImages: Int,
    val totalImages: Int,
    val failed: Boolean = false
)
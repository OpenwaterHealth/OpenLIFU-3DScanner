package health.openwater.openlifu3dscanner.api.dto

import health.openwater.openlifu3dscanner.api.repository.ImageUploader

data class StartPhotoscanRequest(
    val pipelineName: String? = "default_pipeline",
    val inputResizeWidth: Int? = ImageUploader.IMAGE_WIDTH,
    val useMasks: Boolean? = null,
    val matchingMode: String? = null,
    val windowRadius: Int? = null,
    val numNeighbors: Int? = null,
    val locations: List<List<Float>>? = null,
    val returnDurations: Boolean? = null
)

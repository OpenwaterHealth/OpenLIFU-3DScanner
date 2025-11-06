package health.openwater.openlifu3dscanner.api.dto

import health.openwater.openlifu3dscanner.api.repository.ImageUploader

data class StartPhotoscanRequest(
    val pipelineName: String? = "default_pipeline",
    val inputResizeWidth: Int? = ImageUploader.IMAGE_WIDTH,
    val useMasks: Boolean? = null,
    val matchingMode: String? = "sequential_loop",
    val windowRadius: Int? = 8,
    val numNeighbors: Int? = null,
    val locations: List<List<Float>>? = null,
    val returnDurations: Boolean? = null
)

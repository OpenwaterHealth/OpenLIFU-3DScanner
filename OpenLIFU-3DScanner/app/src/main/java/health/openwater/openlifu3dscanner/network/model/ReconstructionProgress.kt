package health.openwater.openlifu3dscanner.network.model

import health.openwater.openlifu3dscanner.network.dto.PhotoscanStatus

data class ReconstructionProgress(
    val progress: Int,
    val message: String?,
    val status: PhotoscanStatus?
)
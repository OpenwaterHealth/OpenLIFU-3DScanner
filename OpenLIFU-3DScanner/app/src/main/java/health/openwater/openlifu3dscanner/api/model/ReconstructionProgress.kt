package health.openwater.openlifu3dscanner.api.model

import health.openwater.openlifu3dscanner.api.dto.PhotoscanStatus

data class ReconstructionProgress(
    val progress: Int,
    val message: String?,
    val status: PhotoscanStatus?
)
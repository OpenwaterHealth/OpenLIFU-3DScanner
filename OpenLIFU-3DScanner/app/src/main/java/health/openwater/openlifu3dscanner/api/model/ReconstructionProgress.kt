package health.openwater.openlifu3dscanner.api.model

data class ReconstructionProgress(
    val progress: Int,
    val message: String?,
    val status: String?
)
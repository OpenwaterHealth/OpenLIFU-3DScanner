package health.openwater.openlifu3dscanner.core

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object UploadComplete : UploadState()
    object StartingReconstruction : UploadState()
    object Reconstructing : UploadState()
    object ReconstructionComplete : UploadState()
    data class Error(val message: String) : UploadState()
}
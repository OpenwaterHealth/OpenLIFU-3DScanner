package health.openwater.openlifu3dscanner.core

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data object UploadComplete : UploadState()
    data object StartingReconstruction : UploadState()
    data object Reconstructing : UploadState()
    data class Error(val message: String) : UploadState()
}
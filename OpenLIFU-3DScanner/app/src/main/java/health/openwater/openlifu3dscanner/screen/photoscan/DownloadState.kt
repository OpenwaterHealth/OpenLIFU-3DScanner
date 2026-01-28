package health.openwater.openlifu3dscanner.screen.photoscan

sealed class DownloadState {
    data object Init : DownloadState()
    data object Processing : DownloadState()
    data object Downloading : DownloadState()
    data object Success : DownloadState()
    data object Failed : DownloadState()
    data object Offline : DownloadState()
    data object NotProcessed : DownloadState() // Local scan that hasn't been uploaded/processed yet
}


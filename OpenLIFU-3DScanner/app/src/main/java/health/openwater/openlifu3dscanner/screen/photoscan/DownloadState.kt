package health.openwater.openlifu3dscanner.screen.photoscan

sealed class DownloadState {
    data object Init : DownloadState()
    data class Processing(val message: String?) : DownloadState()
    data object NotDownloaded : DownloadState()
    data object Downloading : DownloadState()
    data object Success : DownloadState()
    data object Failed : DownloadState()
    data object Offline : DownloadState()
}


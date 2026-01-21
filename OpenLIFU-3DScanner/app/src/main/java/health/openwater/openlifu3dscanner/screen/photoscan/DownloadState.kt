package health.openwater.openlifu3dscanner.screen.photoscan

sealed class DownloadState {
    object Init : DownloadState()
    object NotDownloaded : DownloadState()
    object Downloading : DownloadState()
    object Success : DownloadState()
    object Failed : DownloadState()
    object Offline : DownloadState()
}


package health.openwater.openlifu3dscanner.api.model

enum class Type {
    PHOTOCOLLECTION,
    PHOTOSCAN
}

data class DownloadingItem(
    val id: Long,
    val type: Type
)

data class DownloadResult(
    val item: DownloadingItem,
    val success: Boolean
)
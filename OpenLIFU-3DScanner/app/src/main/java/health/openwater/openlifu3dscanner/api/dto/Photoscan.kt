package health.openwater.openlifu3dscanner.api.dto

import java.util.Date

enum class PhotoscanStatus {
    STARTED,
    RUNNING,
    FINISHED,
    FAILED,
    STOPPED
}

data class Photoscan(
    val id: Long,
    val accountId: String,
    val photocollectionId: Long,
    val creationDate: Date,
    val status: PhotoscanStatus?,
    val message: String?,
    val progress: Int,
    val statusUpdateDate: Date?
)

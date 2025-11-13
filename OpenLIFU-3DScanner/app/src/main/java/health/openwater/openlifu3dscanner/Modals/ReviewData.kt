package health.openwater.openlifu3dscanner.Modals

import health.openwater.openlifu3dscanner.api.dto.PhotoscanStatus
import java.util.Date

enum class Status {
    LOCAL,
    CLOUD,
    DOWNLOADING,
    UNKNOWN
}

data class ReviewData(
    val date: Date,
    val id: String,
    val count: Int,
    val photoStatus: Status,
    val meshStatus: Status,
    val photocollectionId: Long? = null,
    val photoscanId: Long? = null,
    val photoscanStatus: PhotoscanStatus? = null
)
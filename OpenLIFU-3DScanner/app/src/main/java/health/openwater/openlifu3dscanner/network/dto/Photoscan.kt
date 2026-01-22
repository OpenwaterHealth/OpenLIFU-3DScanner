package health.openwater.openlifu3dscanner.network.dto

import java.util.Date


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

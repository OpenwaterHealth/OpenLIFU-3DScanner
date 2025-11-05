package health.openwater.openlifu3dscanner.api.dto

import java.util.Date

data class Photoscan(
    val id: Long,
    val accountId: String,
    val photocollectionId: Long,
    val creationDate: Date,
    val status: String?,
    val message: String?,
    val progress: Int,
    val statusUpdateDate: Date?
)

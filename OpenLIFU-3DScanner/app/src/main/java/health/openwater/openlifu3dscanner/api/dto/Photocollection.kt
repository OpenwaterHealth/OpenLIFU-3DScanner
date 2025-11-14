package health.openwater.openlifu3dscanner.api.dto

import java.util.Date

data class Photocollection(
    val id: Long,
    val accountId: String,
    val name: String?,
    val creationDate: Date,
    val modificationDate: Date?,
    val photos: List<Photo>?
)
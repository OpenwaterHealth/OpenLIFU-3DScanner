package health.openwater.openlifu3dscanner.api.dto

import java.util.Date

data class Photo(
    val fileName: String,
    val fileSize: Long,
    val modificationDate: Date
)
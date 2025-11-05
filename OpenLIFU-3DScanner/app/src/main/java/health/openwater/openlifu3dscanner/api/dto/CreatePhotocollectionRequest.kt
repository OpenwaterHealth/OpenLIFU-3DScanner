package health.openwater.openlifu3dscanner.api.dto

data class CreatePhotocollectionRequest(
    val accountId: String,
    val name: String?
)
package health.openwater.openlifu3dscanner.network.dto

data class CreatePhotocollectionRequest(
    val accountId: String,
    val name: String?
)
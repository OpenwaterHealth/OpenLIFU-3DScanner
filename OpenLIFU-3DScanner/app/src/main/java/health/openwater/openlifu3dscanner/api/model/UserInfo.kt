package health.openwater.openlifu3dscanner.api.model

data class UserInfo(
    val uid: String,
    val displayName: String,
    val email: String,
    val credits: Int
)
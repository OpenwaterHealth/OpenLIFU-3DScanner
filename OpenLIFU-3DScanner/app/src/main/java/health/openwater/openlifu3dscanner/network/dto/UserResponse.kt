package health.openwater.openlifu3dscanner.network.dto

data class UserInfo(
    val uid: String,
    val credit: Int,
    val creationDate: String?,
    val databaseId: Int?,
    val displayName: String?,
    val email: String?,
    val institutionId: Int?,
    val institutionName: String?,
    val isActive: Boolean?,
    val modificationDate: String?,
    val role: String?,
    val scopes: List<String>?,
    val version: Int?
)

data class UserResponseData(
    val user: UserInfo
)

data class UserResponse(
    val status: String,
    val data: UserResponseData?
)

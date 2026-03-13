package health.openwater.openlifu3dscanner.core

data class AuthUser(val uid: String)

sealed class UserState {
    data object Loading : UserState()
    data object Unauthenticated : UserState()
    data class Authenticated(val uid: String) : UserState()
}

data class UserInfoState(
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val credits: Int? = null,
    val institutionName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

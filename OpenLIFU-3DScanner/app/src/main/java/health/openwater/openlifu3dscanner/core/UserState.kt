package health.openwater.openlifu3dscanner.core

import com.google.firebase.auth.FirebaseUser

sealed class UserState {
    data object Loading : UserState()
    data object Unauthenticated : UserState()
    data class Authenticated(val user: FirebaseUser) : UserState()
}

data class UserInfoState(
    val user: FirebaseUser? = null,
    val credits: Int? = null,
    val institutionName: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserInfoState(
    val user: FirebaseUser? = null,
    val credits: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UserViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UserInfoState())
    val uiState = _uiState.asStateFlow()

    fun getCredits() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val currentUser = userRepository.getCurrentUser()

        when (val result = userRepository.getCredits()) {
            is Result.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    credits = result.body.data?.user?.credit,
                    user = currentUser
                )
            }

            is Result.NetworkError -> _uiState.update {
                it.copy(
                    isLoading = false,
                    error = result.message ?: "Network error",
                    user = currentUser
                )
            }

            is Result.AuthError -> _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Authentication required",
                    user = currentUser
                )
            }

            is Result.ServerError -> _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Server error: ${result.code}",
                    user = currentUser
                )
            }

            is Result.UnexpectedError -> _uiState.update {
                it.copy(
                    isLoading = false,
                    error = result.message ?: "Unexpected error",
                    user = currentUser
                )
            }
        }
    }

    suspend fun initialize() = userRepository.initialize()

    fun signOut() {
        userRepository.signOut()
        _uiState.update { it.copy(user = null) }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): AuthService.AuthResponse {
        val response = userRepository.signIn(email, password)
        if (response == AuthService.AuthResponse.SUCCESS) {
            userRepository.getCredits()
        }
        return response
    }

    suspend fun resetPassword(email: String): Boolean {
        return userRepository.resetPassword(email)
    }
}
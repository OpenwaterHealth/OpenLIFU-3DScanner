package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.AuthService
import health.openwater.openlifu3dscanner.api.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun refreshUserInfo() {
        viewModelScope.launch {
            if (userRepository.isSignedIn()) {
                userRepository.refreshUserInfo()
            }
            userRepository.checkCloudAvailability()
            _isLoading.value = false
        }
    }

    fun getCloudAvailability() = userRepository.getCloudAvailability()

    fun getUserInfo() = userRepository.getUserInfo()

    fun signOut() {
        userRepository.signOut()
    }

    suspend fun signIn(
        email: String,
        password: String
    ): AuthService.AuthResponse {
        val response = userRepository.signIn(email, password)
        if (response == AuthService.AuthResponse.SUCCESS) {
            userRepository.refreshUserInfo()
        }
        return response
    }

    suspend fun resetPassword(email: String): Boolean {
        return userRepository.resetPassword(email)
    }
}
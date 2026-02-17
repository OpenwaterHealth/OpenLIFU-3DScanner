package health.openwater.openlifu3dscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.core.UserInfoState
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<UserInfoState> = userRepository.userInfoState
    val isConnected: StateFlow<Boolean> = userRepository.isConnected

    fun getCredits() = viewModelScope.launch {
        userRepository.refreshCredits()
    }

    suspend fun initialize() = userRepository.initialize()

    // Track whether notice was already dismissed this session (without acknowledging)
    // Resets when the user changes so a new user still sees the notice
    private var noticeDismissedThisSession = false

    init {
        viewModelScope.launch {
            uiState.distinctUntilChangedBy { it.user?.uid }.collect {
                noticeDismissedThisSession = false
            }
        }
    }

    val shouldShowNotice: Boolean get() = !noticeDismissedThisSession && userRepository.shouldShowNotice

    fun noticeDismissed() {
        noticeDismissedThisSession = true
    }

    fun noticeAcknowledged() {
        userRepository.noticeAcknowledged()
        noticeDismissedThisSession = true
    }

    fun signOut() = userRepository.signOut()

    suspend fun signIn(email: String, password: String): AuthService.AuthResponse {
        return userRepository.signIn(email, password)
    }

    suspend fun resetPassword(email: String): Boolean {
        return userRepository.resetPassword(email) is Result.Success
    }
}
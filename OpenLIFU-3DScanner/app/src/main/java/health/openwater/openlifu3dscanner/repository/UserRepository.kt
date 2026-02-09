package health.openwater.openlifu3dscanner.repository

import android.util.Log
import health.openwater.openlifu3dscanner.core.ConnectivityObserver
import health.openwater.openlifu3dscanner.core.UserInfoState
import health.openwater.openlifu3dscanner.core.UserState
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.UserService
import health.openwater.openlifu3dscanner.network.dto.ResetPasswordRequest
import health.openwater.openlifu3dscanner.network.dto.StatusResponse
import health.openwater.openlifu3dscanner.network.dto.UserCreditsResponse
import health.openwater.openlifu3dscanner.network.safeCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    val authService: AuthService,
    val userService: UserService,
    val photocollectionService: PhotocollectionService,
    private val connectivityObserver: ConnectivityObserver
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    private val _credits = MutableStateFlow<Int?>(null)
    val credits: StateFlow<Int?> = _credits.asStateFlow()

    val isConnected: StateFlow<Boolean> = connectivityObserver.isConnected

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val userInfoState: StateFlow<UserInfoState> = combine(
        _userState,
        _credits,
        _error
    ) { userState, credits, error ->
        UserInfoState(
            user = (userState as? UserState.Authenticated)?.user,
            credits = credits,
            isLoading = userState is UserState.Loading,
            error = error
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserInfoState()
    )

    init {
        scope.launch {
            connectivityObserver.isConnected.collect { connected ->
                if (connected && _credits.value == null && authService.isSignedIn()) {
                    Log.d(TAG, "Connectivity restored, refreshing credits")
                    refreshCredits()
                }
            }
        }
    }

    suspend fun initialize() {
        _userState.value = UserState.Loading
        authService.initialize()
        val currentUser = authService.getCurrentUser()
        _userState.value = if (currentUser != null) {
            UserState.Authenticated(currentUser)
        } else {
            UserState.Unauthenticated
        }
    }

    fun isSignedIn() = authService.isSignedIn()
    fun getCurrentUser() = authService.getCurrentUser()

    suspend fun signIn(email: String, password: String): AuthService.AuthResponse {
        _userState.value = UserState.Loading
        _error.value = null
        val response = authService.signIn(email, password)
        if (response == AuthService.AuthResponse.SUCCESS) {
            _userState.value = UserState.Authenticated(authService.getCurrentUser()!!)
            refreshCredits()
        } else {
            _userState.value = UserState.Unauthenticated
        }
        return response
    }

    suspend fun isCloudAvailable(): Boolean {
        return try {
            val response = photocollectionService.healthCheck()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getCredits(): Result<UserCreditsResponse> {
        val uid = authService.getCurrentUser()?.uid ?: return Result.AuthError
        return safeCall { userService.getCredits(uid) }
    }

    suspend fun refreshCredits() {
        val currentUser = authService.getCurrentUser()
        if (currentUser != null) {
            _userState.value = UserState.Authenticated(currentUser)
        }

        when (val result = getCredits()) {
            is Result.Success -> {
                _credits.value = result.body.data?.user?.credit
                _error.value = null
            }
            is Result.NetworkError -> _error.value = result.message ?: "Network error"
            is Result.AuthError -> _error.value = "Authentication required"
            is Result.ServerError -> _error.value = "Server error: ${result.code}"
            is Result.UnexpectedError -> _error.value = result.message ?: "Unexpected error"
        }
    }

    fun signOut() {
        authService.signOut()
        _userState.value = UserState.Unauthenticated
        _credits.value = null
        _error.value = null
    }

    suspend fun resetPassword(email: String): Result<StatusResponse> {
        return safeCall { userService.resetPassword(ResetPasswordRequest(email)) }
    }

    companion object {
        private val TAG = UserRepository::class.simpleName
    }
}
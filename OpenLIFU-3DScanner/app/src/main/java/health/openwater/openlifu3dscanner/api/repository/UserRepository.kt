package health.openwater.openlifu3dscanner.api.repository

import android.util.Log
import health.openwater.openlifu3dscanner.api.AuthService
import health.openwater.openlifu3dscanner.api.PhotocollectionService
import health.openwater.openlifu3dscanner.api.UserService
import health.openwater.openlifu3dscanner.api.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserRepository(
    val authService: AuthService,
    val userService: UserService,
    val photocollectionService: PhotocollectionService
) {
    private val userInfoFlow = MutableStateFlow<UserInfo?>(null)
    private val cloudAvailabilityFlow = MutableStateFlow(false)

    fun getCloudAvailability(): StateFlow<Boolean> = cloudAvailabilityFlow

    suspend fun isCloudAvailable(): Boolean {
        return try {
            val response = photocollectionService.healthCheck()
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun checkCloudAvailability() {
        cloudAvailabilityFlow.value = isCloudAvailable()
    }

    fun getUserInfo(): StateFlow<UserInfo?> = userInfoFlow

    suspend fun refreshUserInfo() {
        val currentUser = authService.getCurrentUser() ?: return
        userInfoFlow.value = try {
            val credits = userService.getCredits(currentUser.uid).body()?.data?.user?.credit ?: 0

            UserInfo(
                uid = currentUser.uid,
                displayName = currentUser.displayName ?: "",
                email = currentUser.email ?: "",
                credits = credits
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh user info: ${e.message}")
            null
        }
    }

    fun signOut() {
        authService.signOut()
        userInfoFlow.value = null
    }

    companion object {
        private val TAG = UserRepository::class.java.simpleName
    }
}
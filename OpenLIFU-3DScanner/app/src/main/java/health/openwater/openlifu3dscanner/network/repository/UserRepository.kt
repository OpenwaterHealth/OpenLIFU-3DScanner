package health.openwater.openlifu3dscanner.network.repository

import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.api.AuthService
import health.openwater.openlifu3dscanner.network.api.PhotocollectionService
import health.openwater.openlifu3dscanner.network.api.UserService
import health.openwater.openlifu3dscanner.network.dto.ResetPasswordRequest
import health.openwater.openlifu3dscanner.network.dto.UserCreditsResponse
import health.openwater.openlifu3dscanner.network.safeCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    val authService: AuthService,
    val userService: UserService,
    val photocollectionService: PhotocollectionService
) {
    suspend fun initialize() = authService.initialize()

    fun isSignedIn() = authService.isSignedIn()
    fun getCurrentUser() = authService.getCurrentUser()

    suspend fun signIn(email: String, password: String) = authService.signIn(email, password)

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

    fun signOut() = authService.signOut()

    suspend fun resetPassword(email: String): Boolean {
        return try {
            val response = userService.resetPassword(ResetPasswordRequest(email))
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
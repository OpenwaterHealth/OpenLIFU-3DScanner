package health.openwater.openlifu3dscanner.network.api

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import health.openwater.openlifu3dscanner.core.AuthUser
import health.openwater.openlifu3dscanner.network.dto.AuthLoginRequest
import health.openwater.openlifu3dscanner.network.dto.AuthRefreshRequest
import health.openwater.openlifu3dscanner.preferences.Prefs
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authApi: AuthApi
) {
    enum class AuthResponse {
        SUCCESS,
        INVALID_CREDENTIALS,
        NETWORK_ERROR,
        UNKNOWN
    }

    @Volatile
    private var idToken: String? = null

    @Volatile
    private var tokenExpirationMs: Long = 0

    @Volatile
    private var isInitialized = false

    /**
     * Load stored tokens into memory. Call early at app startup.
     */
    suspend fun initialize() {
        if (isInitialized) return
        val accessToken = Prefs.getAccessToken(context) ?: return
        idToken = accessToken
        tokenExpirationMs = Prefs.getTokenExpirationMs(context)
        isInitialized = true
        Log.d(TAG, "AuthService initialized from stored tokens")
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        return try {
            val response = authApi.login(AuthLoginRequest(email, password)).data
            val expirationMs = response.expirationDate?.time
                ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
            saveTokens(response.accessToken, response.refreshToken, expirationMs, response.uid)
            isInitialized = true
            AuthResponse.SUCCESS
        } catch (e: HttpException) {
            Log.w(TAG, "Sign-in HTTP error: ${e.code()}")
            when (e.code()) {
                400, 401, 403 -> AuthResponse.INVALID_CREDENTIALS
                else -> AuthResponse.NETWORK_ERROR
            }
        } catch (e: IOException) {
            Log.w(TAG, "Sign-in network error", e)
            AuthResponse.NETWORK_ERROR
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in unexpected error", e)
            AuthResponse.UNKNOWN
        }
    }

    fun signOut() {
        idToken = null
        tokenExpirationMs = 0
        isInitialized = false
        Prefs.clearAuthTokens(context)
    }

    suspend fun signOutAndAwait() {
        signOut()
    }

    fun isSignedIn() = Prefs.getRefreshToken(context) != null

    fun getCurrentUser(): AuthUser? {
        val uid = Prefs.getUserUid(context) ?: return null
        if (!isSignedIn()) return null
        return AuthUser(uid)
    }

    suspend fun getToken(): String? {
        if (!isInitialized) initialize()

        val now = System.currentTimeMillis()
        if (idToken == null || now > tokenExpirationMs - TOKEN_REFRESH_BUFFER_MS) {
            val refreshToken = Prefs.getRefreshToken(context) ?: run {
                Log.w(TAG, "No refresh token available")
                return null
            }
            try {
                val response = authApi.refreshToken(AuthRefreshRequest(refreshToken)).data
                val expirationMs = response.expirationDate?.time
                    ?: (now + DEFAULT_TOKEN_LIFETIME_MS)
                saveTokens(response.accessToken, response.refreshToken, expirationMs, response.uid)
                Log.d(TAG, "Token refreshed successfully")
            } catch (e: HttpException) {
                Log.w(TAG, "Token refresh HTTP error: ${e.code()}", e)
                if (e.code() == 400 || e.code() == 401) {
                    // Refresh token is invalid/expired — sign out
                    signOut()
                }
                return null
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed", e)
                return null
            }
        }
        return idToken
    }

    /**
     * Returns the cached access token synchronously without blocking.
     * Used by OkHttp interceptor to avoid deadlocks.
     */
    fun getTokenSync(): String? = idToken

    private fun saveTokens(accessToken: String, refreshToken: String, expirationMs: Long, uid: String) {
        idToken = accessToken
        tokenExpirationMs = expirationMs
        Prefs.saveAuthTokens(context, accessToken, refreshToken, expirationMs, uid)
    }

    companion object {
        private val TAG = AuthService::class.java.simpleName
        private const val DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L  // 1 hour
        private const val TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000L  // refresh 5 min before expiry
    }
}

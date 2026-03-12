package health.openwater.openlifu3dscanner.network.api

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.qualifiers.ApplicationContext
import health.openwater.openlifu3dscanner.preferences.ApiEnvironment
import health.openwater.openlifu3dscanner.preferences.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AuthService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    enum class AuthResponse {
        SUCCESS,
        INVALID_CREDENTIALS,
        NETWORK_ERROR,
        UNKNOWN
    }

    private val auth get() = when (Prefs.getApiEnv(context)) {
        ApiEnvironment.DEV -> FirebaseAuth.getInstance(FirebaseApp.getInstance("dev"))
        else -> FirebaseAuth.getInstance()
    }

    @Volatile
    private var idToken: String? = null

    @Volatile
    private var tokenExpirationTimestamp: Long = 0

    @Volatile
    private var isInitialized = false

    init {
        // Listen for auth state changes
        auth.addIdTokenListener(FirebaseAuth.IdTokenListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            user?.getIdToken(false)?.addOnSuccessListener { result ->
                idToken = result.token
                tokenExpirationTimestamp = result.expirationTimestamp
                Log.d(TAG, "Token updated via listener")
            }
        })
    }

    /**
     * Initialize the auth service by fetching the current token.
     * Call this early in app startup (e.g., Application.onCreate or splash screen).
     */
    suspend fun initialize() {
        if (isInitialized) return

        auth.currentUser?.let { user ->
            try {
                val result = user.getIdToken(false).await()
                idToken = result.token
                tokenExpirationTimestamp = result.expirationTimestamp
                isInitialized = true
                Log.d(TAG, "AuthService initialized with token")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize token", e)
            }
        }
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user != null) {
                // Immediately get fresh token after sign in
                getToken()
                isInitialized = true
                AuthResponse.SUCCESS
            } else {
                AuthResponse.UNKNOWN
            }
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            AuthResponse.INVALID_CREDENTIALS
        } catch (_: Exception) {
            AuthResponse.NETWORK_ERROR
        }
    }

    fun signOut() {
        auth.signOut()
        idToken = null
        tokenExpirationTimestamp = 0
        isInitialized = false
    }

    suspend fun signOutAndAwait() {
        suspendCancellableCoroutine { continuation ->
            var listener: FirebaseAuth.AuthStateListener? = null
            listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser == null && continuation.isActive) {
                    auth.removeAuthStateListener(listener!!)
                    continuation.resume(Unit)
                }
            }
            auth.addAuthStateListener(listener)
            continuation.invokeOnCancellation { auth.removeAuthStateListener(listener) }
            auth.signOut()
        }
        // Auth state changed in memory; give Firebase time to flush its SharedPreferences to disk
        delay(300)
        idToken = null
        tokenExpirationTimestamp = 0
        isInitialized = false
    }

    fun isSignedIn() = auth.currentUser != null

    fun getCurrentUser() = auth.currentUser

    suspend fun getToken(): String? {
        // Ensure initialization happened
        if (!isInitialized) {
            initialize()
        }

        val now = Date().time / 1000
        if (now > tokenExpirationTimestamp || idToken == null) {
            try {
                val response = auth.currentUser?.getIdToken(true)?.await()
                tokenExpirationTimestamp = response?.expirationTimestamp ?: 0L
                idToken = response?.token
                Log.d(TAG, "Token refreshed, expires in ${tokenExpirationTimestamp - now} seconds")
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.w(TAG, e.message ?: "Invalid user")
                signOut()
                idToken = null
                tokenExpirationTimestamp = 0
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing token", e)
                idToken = null
            }
        }
        return idToken
    }

    /**
     * Returns the cached token synchronously without blocking.
     * Used by OkHttp interceptor to avoid deadlocks.
     * Token is kept fresh by:
     * - IdTokenListener for auth state changes
     * - initialize() or getToken() calls throughout the app
     */
    fun getTokenSync(): String? = idToken

    companion object {
        private val TAG = AuthService::class.java.simpleName
    }
}
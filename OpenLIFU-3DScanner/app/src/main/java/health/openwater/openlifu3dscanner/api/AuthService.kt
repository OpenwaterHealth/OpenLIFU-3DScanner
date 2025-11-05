package health.openwater.openlifu3dscanner.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import java.util.Date

class AuthService {
    enum class AuthResponse {
        SUCCESS,
        INVALID_CREDENTIALS,
        NETWORK_ERROR,
        UNKNOWN
    }


    private val auth = FirebaseAuth.getInstance()
    private var idToken: String? = null
    private var tokenExpirationTimestamp: Long = 0

    init {
        auth.addIdTokenListener { auth: FirebaseAuth ->
            val user = auth.currentUser
            user?.getIdToken(false)?.addOnSuccessListener { result ->
                idToken = result.token
            }
        }
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        return try {
            if (auth.signInWithEmailAndPassword(email, password).await().user != null)
                AuthResponse.SUCCESS
            else
                AuthResponse.UNKNOWN
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
    }

    fun isSignedIn(): Boolean {
        return getCurrentUser() != null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun getToken(): String? {
        val now = Date().time / 1000
        if (now > tokenExpirationTimestamp || idToken == null) {
            try {
                val response = auth.currentUser?.getIdToken(true)?.await()
                tokenExpirationTimestamp = response?.expirationTimestamp ?: 0L
                idToken = response?.token
                Log.d(TAG, "Token expire in ${tokenExpirationTimestamp - now} seconds")
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.w(TAG, e.message ?: "Invalid user")
                signOut()
                idToken = null
                tokenExpirationTimestamp = 0
            }
        }
        return idToken
    }

    companion object {
        private val TAG = AuthService::class.java.simpleName
    }

}
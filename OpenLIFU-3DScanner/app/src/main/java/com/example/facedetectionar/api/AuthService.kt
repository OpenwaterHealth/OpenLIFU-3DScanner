package com.example.facedetectionar.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthService {
    enum class AuthResponse {
        SUCCESS,
        INVALID_CREDENTIALS,
        NETWORK_ERROR,
        UNKNOWN
    }


    private val auth = FirebaseAuth.getInstance()
    private var idToken: String? = null

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
    }

    fun isSignedIn(): Boolean {
        return getCurrentUser() != null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun getToken(): String? {
        return idToken ?: run {
            try {
                auth.currentUser?.getIdToken(true)?.await()?.token
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.w(TAG, e.message ?: "Invalid user")
                signOut()
                null
            }
        }
    }

    companion object {
        private val TAG = AuthService::class.java.simpleName
    }

}
package com.example.facedetectionar.api

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthService {
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

    suspend fun signIn(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, password).await()
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
        return idToken ?: auth.currentUser?.getIdToken(true)?.await()?.token
    }

    companion object {
        private val TAG = AuthService::class.java.simpleName
    }

}
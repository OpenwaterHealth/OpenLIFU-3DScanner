package com.example.facedetectionar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.facedetectionar.api.AuthService
import com.example.facedetectionar.api.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity: AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val cancelButton = findViewById<Button>(R.id.cancelBtn)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        cancelButton.setOnClickListener {
            startActivity(Intent(application, welcomeActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
            finish()
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            lifecycleScope.launch {
                val result = userRepository.authService.signIn(username, password)
                if (result.user == null) {

                } else {

                }

                cancelButton.performClick()
            }

        }
    }

}
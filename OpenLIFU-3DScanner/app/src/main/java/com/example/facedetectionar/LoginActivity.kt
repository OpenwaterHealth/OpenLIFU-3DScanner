package com.example.facedetectionar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.facedetectionar.api.AuthService
import com.example.facedetectionar.api.repository.UserRepository
import com.example.facedetectionar.dialogs.LoginErrorDialog
import com.example.facedetectionar.dialogs.LoginSuccessfulDialog
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
        val loadingLayout = findViewById<FrameLayout>(R.id.layoutLoading)

        cancelButton.setOnClickListener {
            finishAndOpenWelcomeActivity()
        }

        loginButton.setOnClickListener {
            loadingLayout.visibility = View.VISIBLE

            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            lifecycleScope.launch {
                val result = userRepository.authService.signIn(username, password)

                when (result) {
                    AuthService.AuthResponse.SUCCESS -> {
                        userRepository.refreshUserInfo()
                        loadingLayout.visibility = View.GONE
                        val dialog = LoginSuccessfulDialog()
                        dialog.show(supportFragmentManager, LoginSuccessfulDialog::class.simpleName)
                    }
                    else -> {
                        loadingLayout.visibility = View.GONE
                        val dialog = LoginErrorDialog(result)
                        dialog.show(supportFragmentManager, LoginErrorDialog::class.simpleName)
                    }
                }
            }

        }
    }

    fun finishAndOpenWelcomeActivity() {
        startActivity(Intent(application, welcomeActivity::class.java).also {
            it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        })
        finish()
    }

}
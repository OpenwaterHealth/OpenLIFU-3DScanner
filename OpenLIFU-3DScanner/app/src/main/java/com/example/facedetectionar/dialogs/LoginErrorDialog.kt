package com.example.facedetectionar.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.facedetectionar.R
import com.example.facedetectionar.api.AuthService

class LoginErrorDialog(
    private val authResponse: AuthService.AuthResponse
): DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.modal_cloud_login_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val errorText = requireView().findViewById<TextView>(R.id.errorText)
        val continueButton = view.findViewById<Button>(R.id.continueBtn)

        continueButton.setOnClickListener {
            dismiss()
        }

        errorText.text = getString(when (authResponse) {
            AuthService.AuthResponse.INVALID_CREDENTIALS -> R.string.incorrect_username_or_password
            else -> R.string.check_internet_connection
        })
    }
}
package com.example.facedetectionar.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.facedetectionar.LoginActivity
import com.example.facedetectionar.R
import com.example.facedetectionar.api.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginSuccessfulDialog: DialogFragment() {
    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.modal_cloud_login_success, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userNameText = requireView().findViewById<TextView>(R.id.userNameText)
        val userInfoText = requireView().findViewById<TextView>(R.id.userInfoText)
        val continueButton = view.findViewById<Button>(R.id.continueBtn)

        continueButton.setOnClickListener {
            dismiss()
            (requireActivity() as LoginActivity?)?.finishAndOpenWelcomeActivity()
        }

        userRepository.getUserInfo().value?.let {
            userNameText.text = it.displayName
            userInfoText.text =
                getString(
                    R.string.email_credits,
                    it.email, it.credits
                )
        }
    }
}
package health.openwater.openlifu3dscanner.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import health.openwater.openlifu3dscanner.LoginActivity
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.api.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CloudStatusDialog: DialogFragment() {

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
        return inflater.inflate(R.layout.modal_cloud_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logInButton = view.findViewById<Button>(R.id.loginBtn)
        val cancelButton = view.findViewById<Button>(R.id.cancelBtn)

        lifecycleScope.launch {
            userRepository.getCloudAvailability().flowWithLifecycle(lifecycle).collect {
                refreshUI()
            }
        }

        lifecycleScope.launch {
            userRepository.getUserInfo().flowWithLifecycle(lifecycle).collect {
                refreshUI()
            }
        }

        logInButton.setOnClickListener {
            if (userRepository.authService.isSignedIn()) {
                userRepository.signOut()
            } else {
                requireContext().startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        lifecycleScope.launch {
            userRepository.checkCloudAvailability()
            userRepository.refreshUserInfo()
        }
    }

    private fun refreshUI() {
        val cloudComputeText = requireView().findViewById<TextView>(R.id.cloudComputeText)
        val cloudStatusText = requireView().findViewById<TextView>(R.id.cloudStatusText)
        val userInfoText = requireView().findViewById<TextView>(R.id.userInfoText)
        val logInButton = requireView().findViewById<Button>(R.id.loginBtn)

        var statusColor = R.color.light_green

        cloudStatusText.setText(
            when {
                !userRepository.getCloudAvailability().value -> {
                    logInButton.isEnabled = false
                    statusColor = R.color.red
                    R.string.not_available
                }
                userRepository.authService.isSignedIn() -> {
                    R.string.connected
                }
                else -> {
                    R.string.available
                }
            }
        )

        cloudComputeText.setTextColor(requireContext().getColor(statusColor))
        cloudStatusText.setTextColor(requireContext().getColor(statusColor))

        userRepository.getUserInfo().value?.let {
            userInfoText.text =
                getString(
                    R.string.user_name_email_credits,
                    it.displayName, it.email, it.credits
                )
            logInButton.setText(R.string.log_out)
        } ?: run {
            userInfoText.setText(R.string.not_logged_in)
            logInButton.setText(R.string.log_in)
        }
    }

}
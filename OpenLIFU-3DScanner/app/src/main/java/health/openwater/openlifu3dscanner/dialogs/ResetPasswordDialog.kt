package health.openwater.openlifu3dscanner.dialogs

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.api.repository.UserRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ResetPasswordDialog: DialogFragment() {

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
        return inflater.inflate(R.layout.modal_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = requireView().findViewById<EditText>(R.id.emailInput)
        val continueButton = view.findViewById<Button>(R.id.continueBtn)
        val cancelButton = view.findViewById<Button>(R.id.cancelBtn)
        val spinner = view.findViewById<View>(R.id.spinner_layout)

        cancelButton.setOnClickListener {
            dismiss()
        }

        emailInput.addTextChangedListener {
            continueButton.isEnabled = Patterns.EMAIL_ADDRESS.matcher(emailInput.text).matches()
        }

        continueButton.isEnabled = false

        continueButton.setOnClickListener {
            spinner.visibility = View.VISIBLE
            lifecycleScope.launch {
                val success = userRepository.resetPassword(emailInput.text.toString())

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    val message = if (success)
                        getString(R.string.please_check_your_inbox_for_a_password_reset_link_if_you_don_t_see_it_check_your_spam_folder)
                    else
                        getString(R.string.there_was_a_problem_sending_the_reset_email_please_try_again)

                    MessageDialog(message).show(parentFragmentManager, MessageDialog::class.simpleName)

                    spinner.visibility = View.GONE

                    if (success) dismiss()
                }
            }
        }
    }

}
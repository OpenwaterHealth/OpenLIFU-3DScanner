package health.openwater.openlifu3dscanner.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.api.repository.UserRepository
import javax.inject.Inject

@AndroidEntryPoint
class NotEnoughCreditsDialog: DialogFragment() {

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
        return inflater.inflate(R.layout.modal_generic_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val errorTitle = requireView().findViewById<TextView>(R.id.errorTitle)
        val errorText = requireView().findViewById<TextView>(R.id.errorText)
        val continueButton = view.findViewById<Button>(R.id.continueBtn)

        val credits = userRepository.getUserInfo().value?.credits ?: 0

        errorTitle.text = getString(R.string.not_enough_credits)
        errorText.text = getString(R.string.you_have_credits_please_contact_openwater, credits)

        continueButton.setOnClickListener {
            dismiss()
        }
    }

}
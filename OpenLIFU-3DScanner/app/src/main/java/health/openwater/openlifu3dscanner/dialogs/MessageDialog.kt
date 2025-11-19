package health.openwater.openlifu3dscanner.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import health.openwater.openlifu3dscanner.R

class MessageDialog(private val message: String): DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.modal_generic_message, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val messageText = view.findViewById<TextView>(R.id.messageText)
        val continueButton = view.findViewById<Button>(R.id.continueBtn)
        messageText.text = message
        continueButton.setOnClickListener {
            dismiss()
        }
    }

}
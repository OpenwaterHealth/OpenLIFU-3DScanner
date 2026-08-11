package health.openwater.openlifu3dscanner.screen.create


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import health.openwater.openlifu3dscanner.R

@Composable
fun SessionNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val idPattern = Regex("[A-Z0-9 _\\-]*")
    OutlinedTextField(
        value = value,
        onValueChange = {
            val upper = it.uppercase(); if (upper.length <= 10 && upper.matches(idPattern)) onValueChange(
            upper
        )
        },
        maxLines = 1,
        label = { Text(stringResource(R.string.input_subject_scan_id)) },
        placeholder = { Text(stringResource(R.string.from_the_desktop_application)) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Ascii
        ),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    )
}
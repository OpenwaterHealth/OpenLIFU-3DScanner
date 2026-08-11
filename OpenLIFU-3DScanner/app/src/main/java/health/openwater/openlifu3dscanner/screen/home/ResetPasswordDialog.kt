package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import health.openwater.openlifu3dscanner.R
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onResetPassword: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = if (showSuccess) stringResource(R.string.email_sent) else stringResource(R.string.reset_password),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showSuccess) {
                    Text(stringResource(R.string.a_password_reset_link_has_been_sent_to_your_email_address_please_check_your_inbox_and_follow_the_instructions))
                } else {
                    Text(stringResource(R.string.enter_your_email_address_and_we_ll_send_you_a_link_to_reset_your_password))

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text(text = stringResource(R.string.email)) },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (showSuccess) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.ok))
                }
            } else {
                TextButton(
                    onClick = {
                        if (email.isEmpty()) {
                            errorMessage = context.getString(R.string.please_enter_your_email)
                            return@TextButton
                        }

                        scope.launch {
                            isLoading = true
                            onResetPassword(email)
                            isLoading = false
                            showSuccess = true
                        }
                    },
                    enabled = !isLoading && email.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.send_reset_link))
                    }
                }
            }
        },
        dismissButton = {
            if (!showSuccess) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    )
}

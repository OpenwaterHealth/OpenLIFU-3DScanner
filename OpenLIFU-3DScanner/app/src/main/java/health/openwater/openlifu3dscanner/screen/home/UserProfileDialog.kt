package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.api.model.UserInfo

@Composable
fun UserProfileDialog(
    userInfo: UserInfo,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = userInfo.displayName,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = userInfo.email)
                Text(text = "Credits: ${userInfo.credits}")
            }
        },
        dismissButton = {
            TextButton(onClick = { showSignOutDialog = true }) {
                Text(
                    text = stringResource(R.string.sign_out),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_to_sign_out)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onDismiss()
                        onSignOut()
                    }
                ) {
                    Text(text = stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

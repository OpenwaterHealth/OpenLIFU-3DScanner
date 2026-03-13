package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@Composable
fun UserProfileDialog(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
) {
    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = uiState.displayName ?: "",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = uiState.email ?: "")
                uiState.institutionName?.let { Text(text = stringResource(R.string.institution_s, it)) }
                Text(text = "Credits: ${uiState.credits}")
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

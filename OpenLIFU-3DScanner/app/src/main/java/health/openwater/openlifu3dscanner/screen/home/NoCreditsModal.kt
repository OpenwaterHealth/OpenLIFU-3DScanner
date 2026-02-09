package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import health.openwater.openlifu3dscanner.R

@Composable
fun NoCreditsModal(
    onDismiss: () -> Unit,
    onProceedOffline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.no_credits_title))
        },
        text = {
            Text(text = stringResource(R.string.no_credits_message))
        },
        confirmButton = {
            TextButton(
                onClick = onProceedOffline
            ) {
                Text(stringResource(R.string.continue_offline))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun NoInternetModal(
    onDismiss: () -> Unit,
    onProceedOffline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.no_internet_title))
        },
        text = {
            Text(text = stringResource(R.string.no_internet_message))
        },
        confirmButton = {
            TextButton(
                onClick = onProceedOffline
            ) {
                Text(stringResource(R.string.continue_offline))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
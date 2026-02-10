package health.openwater.openlifu3dscanner.screen.home

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import health.openwater.openlifu3dscanner.R

@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val email = stringResource(R.string.support_email)
    val phone = stringResource(R.string.support_phone)
    val website = stringResource(R.string.support_website)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.customer_support),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "📧   $email",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())
                                )
                            } catch (_: ActivityNotFoundException) {
                            }
                        }
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "📞   $phone",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_DIAL,
                                        "tel:${phone.replace("[^+\\d]".toRegex(), "")}".toUri()
                                    )
                                )
                            } catch (_: ActivityNotFoundException) {
                            }
                        }
                        .padding(vertical = 4.dp)
                )
                Text(
                    text = "🌐   $website",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, "https://$website".toUri())
                                )
                            } catch (_: ActivityNotFoundException) {
                            }
                        }
                        .padding(vertical = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}

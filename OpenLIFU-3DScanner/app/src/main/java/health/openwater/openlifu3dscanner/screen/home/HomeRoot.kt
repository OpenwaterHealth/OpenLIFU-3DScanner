package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.preferences.ApiEnvironment
import health.openwater.openlifu3dscanner.preferences.Prefs
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

@Composable
fun HomeRoot(
    onStartScan: () -> Unit,
    onViewCollection: () -> Unit,
    onSignIn: () -> Unit,
    onSettings: () -> Unit,
    onSupport: () -> Unit,
) {
    val context = LocalContext.current
    val apiEnv = remember { Prefs.getApiEnv(context) }

    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by userViewModel.isConnected.collectAsStateWithLifecycle()
    val hasCredits = (uiState.credits ?: 0) > 0
    val isLoggedIn = uiState.user != null
    val isOnline = isLoggedIn && hasCredits && isConnected

    var showOfflineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.initialize()
        userViewModel.getCredits()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            LoadingScreen()
        } else {
            WelcomeScreen(
                onStartScan = onStartScan,
                onViewCollection = onViewCollection
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (!isOnline) showOfflineDialog = true }) {
                Icon(
                    imageVector = if (isOnline) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                    contentDescription = stringResource(
                        if (isOnline) R.string.online else R.string.offline
                    )
                )
            }
            IconButton(onClick = onSupport) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = stringResource(R.string.customer_support)
                )
            }
            IconButton(onClick = onSettings) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null)
            }
        }

        UserProfileBadge(
            onSignIn = onSignIn,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        if (apiEnv != ApiEnvironment.PRODUCTION) {
            Surface(
                color = Color(0xFFE65100.toInt()),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Text(
                    text = stringResource(when (apiEnv) {
                        ApiEnvironment.DEV -> R.string.env_dev
                        ApiEnvironment.SANDBOX -> R.string.env_sandbox
                    }),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (showOfflineDialog) {
            val title = if (isLoggedIn) R.string.no_network else R.string.offline_mode
            AlertDialog(
                onDismissRequest = { showOfflineDialog = false },
                title = { Text(stringResource(title)) },
                text = {
                    if (isLoggedIn) {
                        Text(stringResource(R.string.no_network_message))
                    } else {
                        val email = stringResource(R.string.support_email)
                        val fullText = stringResource(R.string.offline_mode_message)
                        val linkColor = MaterialTheme.colorScheme.primary
                        val annotatedString = buildAnnotatedString {
                            val emailStart = fullText.indexOf(email)
                            if (emailStart < 0) {
                                append(fullText)
                            } else {
                                append(fullText.substring(0, emailStart))
                                withLink(
                                    LinkAnnotation.Url(
                                        url = "mailto:$email",
                                        styles = androidx.compose.ui.text.TextLinkStyles(
                                            style = SpanStyle(
                                                color = linkColor,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        )
                                    )
                                ) {
                                    append(email)
                                }
                                append(fullText.substring(emailStart + email.length))
                            }
                        }
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showOfflineDialog = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

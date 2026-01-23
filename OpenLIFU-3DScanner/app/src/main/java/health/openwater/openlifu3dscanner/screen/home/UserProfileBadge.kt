package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import java.util.Locale

@Composable
fun UserProfileBadge(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 32,
) {
    val userViewModel: UserViewModel = hiltViewModel()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) return

    val initial = uiState.user
        ?.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.substring(0, 1)
        ?.uppercase(Locale.getDefault())
        ?: "?"

    if (uiState.user != null) {
        Surface(
            modifier = modifier.clickable { showProfileDialog = true },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Row {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(size.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { showProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "\uD83C\uDFE6 ${uiState.credits ?: "N/A"} ",
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
        }
    } else {
        Surface(
            modifier = modifier.clickable { onSignIn() },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 16.dp)
            )
        }
    }

    if (showProfileDialog && uiState.user != null) {
        UserProfileDialog(
            onDismiss = { showProfileDialog = false },
            onSignOut = { userViewModel.signOut() }
        )
    }
}

package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import java.util.Locale

@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = hiltViewModel(),
    size: Int = 32,
) {
    val userInfo by userViewModel.getUserInfo().collectAsState(initial = null)
    var showProfileDialog by remember { mutableStateOf(false) }

    val initial = userInfo
        ?.displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.substring(0, 1)
        ?.uppercase(Locale.getDefault())
        ?: "?"

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

            // Credits badge
            userInfo?.let {
                Text(
                    text = "\uD83C\uDFE6 ${it.credits} ",
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
        }
    }

    if (showProfileDialog && userInfo != null) {
        UserProfileDialog(
            userInfo = userInfo!!,
            onDismiss = { showProfileDialog = false },
            onSignOut = { userViewModel.signOut() }
        )
    }
}

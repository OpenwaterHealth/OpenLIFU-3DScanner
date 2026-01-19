package health.openwater.openlifu3dscanner.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.preferences.Prefs
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.createPreferenceFlow
import me.zhanghai.compose.preference.listPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = Prefs.getInstance(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { contentPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            ProvidePreferenceLocals(
                flow = createPreferenceFlow(prefs)
            ) {
                LazyColumn {

                    // Image Size Preference
                    listPreference(
                        key = Prefs.IMAGE_SIZE_KEY,
                        defaultValue = Prefs.IMAGE_SIZE_DEFAULT,
                        values = Prefs.IMAGE_SIZE_MAP.keys.toList(),
                        title = { Text("Image Size") },
                        valueToText = { AnnotatedString(Prefs.IMAGE_SIZE_MAP[it] ?: it.toString()) },
                        summary = { Text(Prefs.IMAGE_SIZE_MAP[it] ?: it.toString()) }
                    )

                    // Photo Count Preference
                    listPreference(
                        key = Prefs.PHOTO_COUNT_KEY,
                        defaultValue = Prefs.PHOTO_COUNT_DEFAULT,
                        values = Prefs.PHOTO_COUNT_MAP.keys.toList(),
                        title = { Text("Photo Count") },
                        valueToText = { AnnotatedString(Prefs.PHOTO_COUNT_MAP[it] ?: it.toString()) },
                        summary = { Text(Prefs.PHOTO_COUNT_MAP[it] ?: it.toString()) }
                    )
                }
            }
        }
    }
}

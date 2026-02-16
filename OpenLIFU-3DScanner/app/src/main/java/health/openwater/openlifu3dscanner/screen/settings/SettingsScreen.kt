package health.openwater.openlifu3dscanner.screen.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import health.openwater.openlifu3dscanner.BuildConfig
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.screen.home.NoticeDialog
import health.openwater.openlifu3dscanner.preferences.Prefs
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.createPreferenceFlow
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import androidx.core.net.toUri

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = Prefs.getInstance(context)
    val userViewModel: UserViewModel = hiltViewModel()
    var showNoticeDialog by remember { mutableStateOf(false) }

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

                    // Scan Settings Category
                    preferenceCategory(
                        key = "scan_settings",
                        title = { Text(stringResource(R.string.scan_settings)) }
                    )

                    // Image Size Preference
                    listPreference(
                        key = Prefs.IMAGE_SIZE_KEY,
                        defaultValue = Prefs.IMAGE_SIZE_DEFAULT,
                        values = Prefs.IMAGE_SIZE_MAP.keys.toList(),
                        title = { Text(stringResource(R.string.image_size)) },
                        valueToText = {
                            val resId = Prefs.IMAGE_SIZE_MAP[it]
                            AnnotatedString(if (resId != null) context.getString(resId) else it.toString())
                        },
                        summary = {
                            val resId = Prefs.IMAGE_SIZE_MAP[it]
                            Text(if (resId != null) stringResource(resId) else it.toString())
                        }
                    )

                    // Photo Count Preference
                    listPreference(
                        key = Prefs.PHOTO_COUNT_KEY,
                        defaultValue = Prefs.PHOTO_COUNT_DEFAULT,
                        values = Prefs.PHOTO_COUNT_MAP.keys.toList(),
                        title = { Text(stringResource(R.string.photo_count_setting)) },
                        valueToText = {
                            val resId = Prefs.PHOTO_COUNT_MAP[it]
                            AnnotatedString(if (resId != null) context.getString(resId) else it.toString())
                        },
                        summary = {
                            val resId = Prefs.PHOTO_COUNT_MAP[it]
                            Text(if (resId != null) stringResource(resId) else it.toString())
                        }
                    )

                    // Oval Size Preference
                    listPreference(
                        key = Prefs.OVAL_SIZE_KEY,
                        defaultValue = Prefs.OVAL_SIZE_DEFAULT,
                        values = Prefs.OVAL_SIZE_MAP.keys.toList(),
                        title = { Text(stringResource(R.string.oval_size)) },
                        valueToText = {
                            val resId = Prefs.OVAL_SIZE_MAP[it]
                            AnnotatedString(if (resId != null) context.getString(resId) else it.toString())
                        },
                        summary = {
                            val resId = Prefs.OVAL_SIZE_MAP[it]
                            Text(if (resId != null) stringResource(resId) else it.toString())
                        }
                    )

                    // Capture Mode Preference
                    listPreference(
                        key = Prefs.CAPTURE_MODE_KEY,
                        defaultValue = Prefs.CAPTURE_MODE_DEFAULT,
                        values = Prefs.CAPTURE_MODE_MAP.keys.toList(),
                        title = { Text(stringResource(R.string.capture_mode)) },
                        valueToText = {
                            val resId = Prefs.CAPTURE_MODE_MAP[it]
                            AnnotatedString(if (resId != null) context.getString(resId) else it.toString())
                        },
                        summary = {
                            val resId = Prefs.CAPTURE_MODE_MAP[it]
                            Text(if (resId != null) stringResource(resId) else it.toString())
                        }
                    )

                    // Support & Legal Category
                    preferenceCategory(
                        key = "support_legal",
                        title = { Text(stringResource(R.string.support_and_legal)) }
                    )

                    // Video Tutorial
                    preference(
                        key = "video_tutorial",
                        title = { Text(stringResource(R.string.video_tutorial)) },
                        summary = { Text(stringResource(R.string.video_tutorial_summary)) },
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.youtube.com/watch?v=U_cHAH4T8Co".toUri() // TODO: Replace with actual tutorial video
                            )
                            context.startActivity(intent)
                        }
                    )

                    // Submit Feedback
                    preference(
                        key = "submit_feedback",
                        title = { Text(stringResource(R.string.submit_feedback)) },
                        summary = { Text(stringResource(R.string.submit_feedback_summary)) },
                        onClick = {
                            val deviceInfo = buildString {
                                appendLine("---")
                                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                                appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                                appendLine("---")
                            }
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@openwater.health"))
                                putExtra(Intent.EXTRA_SUBJECT, "OpenLIFU App Feedback")
                                putExtra(Intent.EXTRA_TEXT, "\n\n$deviceInfo")
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    context.getString(R.string.submit_feedback)
                                )
                            )
                        }
                    )

                    // Acknowledge Notice
                    preference(
                        key = "acknowledge_notice",
                        title = { Text(stringResource(R.string.acknowledge_notice)) },
                        summary = { Text(stringResource(R.string.acknowledge_notice_summary)) },
                        onClick = { showNoticeDialog = true }
                    )
                }
            }
        }
    }

    if (showNoticeDialog) {
        NoticeDialog(
            isAcknowledged = !userViewModel.shouldShowNotice,
            onDismiss = { dontShowAgain ->
                showNoticeDialog = false
                if (dontShowAgain) {
                    userViewModel.noticeAcknowledged()
                }
            }
        )
    }
}

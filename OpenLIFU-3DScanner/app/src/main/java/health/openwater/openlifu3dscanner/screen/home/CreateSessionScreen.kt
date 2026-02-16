package health.openwater.openlifu3dscanner.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.network.dto.Session
import health.openwater.openlifu3dscanner.network.dto.SubjectWithSessions
import health.openwater.openlifu3dscanner.preferences.Prefs
import health.openwater.openlifu3dscanner.repository.ScanConfig
import health.openwater.openlifu3dscanner.viewmodel.CloudViewModel
import health.openwater.openlifu3dscanner.viewmodel.HomeViewModel
import health.openwater.openlifu3dscanner.viewmodel.UserViewModel

enum class SessionMode {
    EXISTING,
    NEW
}

@Composable
private fun SessionNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val idPattern = Regex("[a-zA-Z0-9 _\\-]*")
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.matches(idPattern)) onValueChange(it) },
        maxLines = 1,
        label = { Text(stringResource(R.string.new_session_name)) },
        placeholder = { Text(stringResource(R.string.from_the_desktop_application)) },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Ascii
        ),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCollectionScreen(
    onNavigateBack: () -> Unit,
    onStartScan: () -> Unit,
) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()

    val subjectsState by homeViewModel.subjectsState.collectAsStateWithLifecycle()
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()

    val isLoggedIn = uiState.user != null
    val hasCredits = (uiState.credits ?: 0) > 0

    val context = LocalContext.current
    var autoUploadEnabled by remember { mutableStateOf(Prefs.getAutoUpload(context)) }

    var selectedSubject by remember { mutableStateOf<SubjectWithSessions?>(null) }
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var manualSessionName by remember { mutableStateOf("") }

    var sessionMode by remember { mutableStateOf(SessionMode.EXISTING) }

    var subjectExpanded by remember { mutableStateOf(false) }
    var sessionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            homeViewModel.loadSubjects()
        }
    }

    val canStart = if (!isLoggedIn || subjectsState.error != null) {
        manualSessionName.isNotBlank()
    } else when (sessionMode) {
        SessionMode.EXISTING -> selectedSession != null
        SessionMode.NEW -> manualSessionName.isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.create_new_photo_collection)) },
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Button(
                    onClick = {
                        val sessionName: String
                        val sessionId: Long?

                        if (!isLoggedIn || subjectsState.error != null) {
                            sessionName = manualSessionName.trim()
                            sessionId = null
                        } else if (sessionMode == SessionMode.EXISTING) {
                            sessionName = selectedSession!!.name
                            sessionId = selectedSession!!.id
                        } else {
                            sessionName = manualSessionName.trim()
                            sessionId = null
                        }

                        cloudViewModel.setScanConfig(
                            ScanConfig(
                                collectionName = sessionName,
                                autoUploadEnabled = if (hasCredits) autoUploadEnabled else false,
                                sessionId = sessionId
                            )
                        )
                        onStartScan()
                    },
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.next),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            if (!isLoggedIn) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }

                SessionNameField(
                    value = manualSessionName,
                    onValueChange = { manualSessionName = it },
                    focusRequester = focusRequester
                )
                return@Column
            }

            when {
                subjectsState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                subjectsState.error != null -> {
                    Text(
                        text = stringResource(R.string.failed_to_load_subjects),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SessionNameField(
                        value = manualSessionName,
                        onValueChange = { manualSessionName = it }
                    )
                }

                else -> {
                    // ---------- Session mode selector ----------
                    Text(
                        text = stringResource(R.string.subject_id),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SegmentedButton(
                            selected = sessionMode == SessionMode.EXISTING,
                            onClick = {
                                sessionMode = SessionMode.EXISTING
                                manualSessionName = ""
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 0,
                                count = 2
                            )
                        ) {
                            Text(stringResource(R.string.select_existing))
                        }

                        SegmentedButton(
                            selected = sessionMode == SessionMode.NEW,
                            onClick = {
                                sessionMode = SessionMode.NEW
                                selectedSubject = null
                                selectedSession = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = 1,
                                count = 2
                            )
                        ) {
                            Text(stringResource(R.string.manually_input))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------- Session input ----------
                    when (sessionMode) {
                        SessionMode.EXISTING -> {
                            // Subject picker
                            ExposedDropdownMenuBox(
                                expanded = subjectExpanded,
                                onExpandedChange = { subjectExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject?.name.orEmpty(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.select_subject_id)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = subjectExpanded,
                                    onDismissRequest = { subjectExpanded = false }
                                ) {
                                    subjectsState.subjects.forEach { subject ->
                                        DropdownMenuItem(
                                            text = { Text(subject.name) },
                                            onClick = {
                                                selectedSubject = subject
                                                selectedSession = null
                                                subjectExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Session picker
                            ExposedDropdownMenuBox(
                                expanded = sessionExpanded,
                                onExpandedChange = {
                                    if (selectedSubject != null) sessionExpanded = it
                                }
                            ) {
                                OutlinedTextField(
                                    value = selectedSession?.name.orEmpty(),
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = selectedSubject != null,
                                    label = {
                                        Text(stringResource(R.string.select_session_id))
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            sessionExpanded
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = sessionExpanded,
                                    onDismissRequest = { sessionExpanded = false }
                                ) {
                                    selectedSubject?.sessions.orEmpty()
                                        .forEach { session ->
                                            DropdownMenuItem(
                                                text = { Text(session.name) },
                                                onClick = {
                                                    selectedSession = session
                                                    sessionExpanded = false
                                                }
                                            )
                                        }
                                }
                            }
                        }

                        SessionMode.NEW -> {
                            Text(
                                text = stringResource(R.string.manual_input_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            SessionNameField(
                                value = manualSessionName,
                                onValueChange = { manualSessionName = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // ---------- Auto upload ----------
                    if (hasCredits) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    autoUploadEnabled = !autoUploadEnabled
                                    Prefs.setAutoUpload(context, autoUploadEnabled)
                                }
                        ) {
                            Checkbox(
                                checked = autoUploadEnabled,
                                onCheckedChange = {
                                    autoUploadEnabled = it
                                    Prefs.setAutoUpload(context, it)
                                }
                            )
                            Text(
                                text = stringResource(R.string.auto_upload),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

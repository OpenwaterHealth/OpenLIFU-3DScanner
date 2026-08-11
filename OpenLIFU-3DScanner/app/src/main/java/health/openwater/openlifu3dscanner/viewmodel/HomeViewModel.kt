package health.openwater.openlifu3dscanner.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.network.api.SubjectService
import health.openwater.openlifu3dscanner.network.dto.SubjectWithSessions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectsUiState(
    val subjects: List<SubjectWithSessions> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subjectService: SubjectService
) : ViewModel() {

    private val _subjectsState = MutableStateFlow(SubjectsUiState())
    val subjectsState: StateFlow<SubjectsUiState> = _subjectsState.asStateFlow()

    fun loadSubjects() {
        _subjectsState.value = SubjectsUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val subjects = subjectService.getSubjectsWithSessions()
                _subjectsState.value = SubjectsUiState(subjects = subjects)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load subjects", e)
                _subjectsState.value = SubjectsUiState(error = e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}

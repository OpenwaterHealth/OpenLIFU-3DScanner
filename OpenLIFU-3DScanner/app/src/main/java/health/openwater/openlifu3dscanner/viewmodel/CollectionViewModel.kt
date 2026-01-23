package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.Photoscan
import health.openwater.openlifu3dscanner.network.model.DownloadingItem
import health.openwater.openlifu3dscanner.network.model.Type
import health.openwater.openlifu3dscanner.network.repository.CloudRepository
import health.openwater.openlifu3dscanner.network.repository.CollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val loadingPhotocollections: Boolean = true,
    val photocollections: List<Photocollection>? = null,
    val photocollectionsError: String? = null,

    val loadingPhotoscans: Boolean = true,
    val photoscans: List<Photoscan>? = null,
    val photoscansError: String? = null
) {
    val isLoading: Boolean
        get() = loadingPhotocollections || loadingPhotoscans

    val hasError: Boolean
        get() = photocollectionsError != null || photoscansError != null
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository,
    private val collectionRepository: CollectionRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState = _uiState.asStateFlow()

    fun loadPhotocollections() = viewModelScope.launch {
        _uiState.update { it.copy(loadingPhotocollections = true, photocollectionsError = null) }

        when (val result = collectionRepository.getPhotocollections()) {
            is Result.Success -> _uiState.update {
                it.copy(
                    loadingPhotocollections = false,
                    photocollections = result.body
                )
            }

            is Result.NetworkError -> _uiState.update {
                it.copy(
                    loadingPhotocollections = false,
                    photocollectionsError = result.message ?: "Network error"
                )
            }

            is Result.AuthError -> _uiState.update {
                it.copy(
                    loadingPhotocollections = false,
                    photocollectionsError = "Authentication required"
                )
            }

            is Result.ServerError -> _uiState.update {
                it.copy(
                    loadingPhotocollections = false,
                    photocollectionsError = "Server error: ${result.code}"
                )
            }

            is Result.UnexpectedError -> _uiState.update {
                it.copy(
                    loadingPhotocollections = false,
                    photocollectionsError = result.message ?: "Unexpected error"
                )
            }
        }
    }

    fun loadPhotoscans() = viewModelScope.launch {
        _uiState.update { it.copy(loadingPhotoscans = true, photoscansError = null) }

        when (val result = collectionRepository.getPhotoscans()) {
            is Result.Success -> _uiState.update {
                it.copy(
                    loadingPhotoscans = false,
                    photoscans = result.body
                )
            }

            is Result.NetworkError -> _uiState.update {
                it.copy(
                    loadingPhotoscans = false,
                    photoscansError = result.message ?: "Network error"
                )
            }

            is Result.AuthError -> _uiState.update {
                it.copy(
                    loadingPhotoscans = false,
                    photoscansError = "Authentication required"
                )
            }

            is Result.ServerError -> _uiState.update {
                it.copy(
                    loadingPhotoscans = false,
                    photoscansError = "Server error: ${result.code}"
                )
            }

            is Result.UnexpectedError -> _uiState.update {
                it.copy(
                    loadingPhotoscans = false,
                    photoscansError = result.message ?: "Unexpected error"
                )
            }
        }
    }

    suspend fun getPhotocollection(
        photocollectionId: Long,
        joinPhotos: Boolean = false
    ): Photocollection? {
        return cloudRepository.getPhotocollection(
            id = photocollectionId,
            joinPhotos = joinPhotos
        )
    }

    fun downloadMesh(scanId: Long) {
        cloudRepository.download(DownloadingItem(scanId, Type.PHOTOSCAN))
    }

    fun downloadPhotocollection(collectionId: Long) {
        cloudRepository.download(DownloadingItem(collectionId, Type.PHOTOCOLLECTION))
    }

    fun getDownloadResultsFlow() = cloudRepository.getDownloadResultsFlow()

    suspend fun deleteScan(
        photoscanId: Long,
        photocollectionId: Long,
        collectionName: String
    ): Boolean {
        // Delete photoscan from server
        val photoscanDeleted = cloudRepository.deletePhotoscan(photoscanId)

        // Delete photocollection from server
        val photocollectionDeleted = cloudRepository.deletePhotocollection(photocollectionId)

        // Delete local directory
        val localDeleted = cloudRepository.deleteLocalScanDirectory(collectionName)

        return photoscanDeleted && photocollectionDeleted && localDeleted
    }
}
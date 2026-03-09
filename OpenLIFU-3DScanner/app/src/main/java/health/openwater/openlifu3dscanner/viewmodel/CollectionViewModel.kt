package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.network.Result
import health.openwater.openlifu3dscanner.network.dto.Photocollection
import health.openwater.openlifu3dscanner.network.dto.Photoscan
import health.openwater.openlifu3dscanner.network.model.DownloadingItem
import health.openwater.openlifu3dscanner.network.model.Type
import health.openwater.openlifu3dscanner.repository.CloudRepository
import health.openwater.openlifu3dscanner.repository.CollectionRepository
import health.openwater.openlifu3dscanner.repository.ScanOwnershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val loadingPhotocollections: Boolean = true,
    val photocollections: List<Photocollection>? = null,
    val photocollectionsError: String? = null,
    val photocollectionsServerError: Boolean = false,

    val loadingPhotoscans: Boolean = true,
    val photoscans: List<Photoscan>? = null,
    val photoscansError: String? = null,
    val photoscansServerError: Boolean = false,

    /** Names of local scan directories owned by the current user. Null = not yet loaded. */
    val ownedLocalCollections: Set<String>? = null
) {
    val isLoading: Boolean
        get() = loadingPhotocollections || loadingPhotoscans

    val hasError: Boolean
        get() = photocollectionsError != null || photoscansError != null

    /** True only for server/unexpected errors, not for auth/network (offline) errors */
    val hasServerError: Boolean
        get() = photocollectionsServerError || photoscansServerError
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository,
    private val collectionRepository: CollectionRepository,
    private val scanOwnershipRepository: ScanOwnershipRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState = _uiState.asStateFlow()

    fun loadPhotocollections() = viewModelScope.launch {
        _uiState.update { it.copy(loadingPhotocollections = true, photocollectionsError = null, photocollectionsServerError = false) }

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
                    photocollectionsError = "Server error: ${result.code}",
                    photocollectionsServerError = true
                )
            }

            is Result.UnexpectedError -> _uiState.update {
                it.copy(
                    loadingPhotocollections = false,
                    photocollectionsError = result.message ?: "Unexpected error",
                    photocollectionsServerError = true
                )
            }
        }
    }

    fun loadPhotoscans() = viewModelScope.launch {
        _uiState.update { it.copy(loadingPhotoscans = true, photoscansError = null, photoscansServerError = false) }

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
                    photoscansError = "Server error: ${result.code}",
                    photoscansServerError = true
                )
            }

            is Result.UnexpectedError -> _uiState.update {
                it.copy(
                    loadingPhotoscans = false,
                    photoscansError = result.message ?: "Unexpected error",
                    photoscansServerError = true
                )
            }
        }
    }

    /**
     * Claims any unowned local scan directories for the current user (one-time migration),
     * then updates [CollectionUiState.ownedLocalCollections] with the current user's owned names.
     */
    fun loadOwnedLocalCollections() = viewModelScope.launch {
        val localDirNames = getModelsDir(getApplication())
            .listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
        scanOwnershipRepository.claimUnowned(localDirNames)
        val owned = scanOwnershipRepository.getOwnedCollectionNames()
        _uiState.update { it.copy(ownedLocalCollections = owned) }
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

    suspend fun getPhotoscan(photoscanId: Long): Photoscan? {
        return cloudRepository.getPhotoscan(photoscanId)
    }

    fun downloadMesh(scanId: Long) {
        cloudRepository.download(DownloadingItem(scanId, Type.PHOTOSCAN))
    }

    fun downloadPhotocollection(collectionId: Long) {
        cloudRepository.download(DownloadingItem(collectionId, Type.PHOTOCOLLECTION))
    }

    fun getDownloadResultsFlow() = cloudRepository.getDownloadResultsFlow()

    fun getPhotoDownloadProgress() = cloudRepository.photoDownloadProgress

    suspend fun deleteScan(
        photoscanId: Long,
        photocollectionId: Long,
        collectionName: String,
        deleteLocal: Boolean = true,
        deleteCloud: Boolean = true
    ): Boolean {
        var success = true

        if (deleteCloud) {
            if (photoscanId != 0L) {
                success = cloudRepository.deletePhotoscan(photoscanId) && success
            }
            if (photocollectionId != 0L) {
                success = cloudRepository.deletePhotocollection(photocollectionId) && success
            }
        }

        if (deleteLocal) {
            success = cloudRepository.deleteLocalScanDirectory(collectionName) && success
        }

        return success
    }
}
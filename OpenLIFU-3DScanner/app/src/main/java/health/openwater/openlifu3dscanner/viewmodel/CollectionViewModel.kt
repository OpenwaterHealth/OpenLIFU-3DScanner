package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.DomainResult
import health.openwater.openlifu3dscanner.api.dto.Photocollection
import health.openwater.openlifu3dscanner.api.dto.Photoscan
import health.openwater.openlifu3dscanner.api.model.DownloadingItem
import health.openwater.openlifu3dscanner.api.model.Type
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CollectionViewModel @Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository
) : AndroidViewModel(application) {

    private val _photocollectionsResponse =
        MutableStateFlow<DomainResult<List<Photocollection>>?>(null)
    val photocollectionsResponse = _photocollectionsResponse.asStateFlow()

    private val _photoscansResponse =
        MutableStateFlow<DomainResult<List<Photoscan>>?>(null)
    val photoscansResponse = _photoscansResponse.asStateFlow()

    fun getPhotocollections(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _photocollectionsResponse.value = DomainResult.Loading
            _photocollectionsResponse.value = cloudRepository.getPhotocollections()
        }
    }

    fun getPhotoscans(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _photoscansResponse.value = DomainResult.Loading
            _photoscansResponse.value = cloudRepository.getPhotoscans()
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
}
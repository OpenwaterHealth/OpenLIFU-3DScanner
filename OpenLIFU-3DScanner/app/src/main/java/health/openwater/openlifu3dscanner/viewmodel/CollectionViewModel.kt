package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.dto.Photocollection
import health.openwater.openlifu3dscanner.api.dto.Photoscan
import health.openwater.openlifu3dscanner.api.model.DownloadingItem
import health.openwater.openlifu3dscanner.api.model.Type
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class CollectionViewModel @Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository
) : AndroidViewModel(application) {

    private val _photoscans = MutableStateFlow<List<Photoscan>?>(null)
    val photoscans: StateFlow<List<Photoscan>?> = _photoscans.asStateFlow()

    private val _isLoadingPhotoscans = MutableStateFlow(false)
    val isLoadingPhotoscans: StateFlow<Boolean> = _isLoadingPhotoscans.asStateFlow()

    suspend fun getPhotoscans() {
        _isLoadingPhotoscans.value = _photoscans.value == null
        _photoscans.value = cloudRepository.getPhotoscans()?.reversed()
        _isLoadingPhotoscans.value = false
    }

    suspend fun getPhotocollection(photoscanId: Long): Photocollection? {
        val photoscan = cloudRepository.getPhotoscan(photoscanId) ?: return null
        return cloudRepository.getPhotocollection(photoscan.photocollectionId)
    }

    fun downloadMesh(scanId: Long) {
        cloudRepository.download(DownloadingItem(scanId, Type.PHOTOSCAN))
    }

    fun getDownloadResultsFlow() = cloudRepository.getDownloadResultsFlow()
}
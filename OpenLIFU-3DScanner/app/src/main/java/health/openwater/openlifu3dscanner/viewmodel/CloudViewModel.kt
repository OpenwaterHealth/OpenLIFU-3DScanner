package health.openwater.openlifu3dscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.network.repository.CloudRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val cloudRepository: CloudRepository
) : ViewModel() {

    val uploadState = cloudRepository.uploadState
    val imageUploadProgress = cloudRepository.getImageUploadProgress()
    val reconstructionProgress = cloudRepository.getReconstructionProgress()
    val currentPhotoscanId: Long? get() = cloudRepository.currentPhotoscanId

    fun getCurrentPhotocollection() = cloudRepository.getCurrentPhotocollection()

    fun createPhotocollection(collectionName: String, autoUpload: Boolean) {
        cloudRepository.createPhotocollection(
            name = collectionName,
            autoUpload = autoUpload
        )
    }

    fun startReconstruction() {
        viewModelScope.launch {
            cloudRepository.startReconstructionFlow()
        }
    }

    fun uploadRemainingPhotos() = cloudRepository.uploadRemainingPhotos()

    fun isLoggedInAndOnline() = cloudRepository.isLoggedInAndOnline()

    fun reset(removeLocalCollection: Boolean) = cloudRepository.reset(removeLocalCollection)
}
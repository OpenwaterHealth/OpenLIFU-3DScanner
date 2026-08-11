package health.openwater.openlifu3dscanner.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.repository.CloudRepository
import health.openwater.openlifu3dscanner.repository.ScanConfig
import javax.inject.Inject

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val cloudRepository: CloudRepository
) : ViewModel() {

    val uploadState = cloudRepository.uploadState
    val imageUploadProgress = cloudRepository.getImageUploadProgress()
    val reconstructionProgress = cloudRepository.getReconstructionProgress()
    val photocollectionReady = cloudRepository.photocollectionReady
    val currentPhotoscanId: Long? get() = cloudRepository.currentPhotoscanId
    val scanConfig get() = cloudRepository.scanConfig

    fun setScanConfig(config: ScanConfig) = cloudRepository.setScanConfig(config)

    fun getCurrentPhotocollection() = cloudRepository.getCurrentPhotocollection()

    fun createPhotocollection(collectionName: String, autoUpload: Boolean, sessionId: Long? = null) {
        cloudRepository.createPhotocollection(
            name = collectionName,
            autoUpload = autoUpload,
            sessionId = sessionId
        )
    }

    fun startReconstruction() = cloudRepository.startReconstruction()

    fun uploadRemainingPhotos() = cloudRepository.uploadRemainingPhotos()

    fun onScanComplete() = cloudRepository.onScanComplete()

    fun isLoggedInAndOnline() = cloudRepository.isLoggedInAndOnline()

    fun dismissCurrentSession() = cloudRepository.dismissCurrentSession()

    fun reset(removeLocalCollection: Boolean) = cloudRepository.reset(removeLocalCollection)

    /**
     * Reset and recreate the photocollection (used for recapture).
     * Cancels and deletes the current session, then starts a fresh one.
     */
    fun resetPhotocollection(collectionName: String, autoUpload: Boolean, sessionId: Long? = null) {
        cloudRepository.createPhotocollection(
            name = collectionName,
            autoUpload = autoUpload,
            sessionId = sessionId,
            cancelPrevious = true
        )
    }
}

package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.api.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.api.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import health.openwater.openlifu3dscanner.core.UploadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CloudViewModel @Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository
) : AndroidViewModel(application) {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _imageUploadProgress = MutableStateFlow<ImageUploadProgress?>(null)
    val imageUploadProgress: StateFlow<ImageUploadProgress?> = _imageUploadProgress.asStateFlow()

    private val _reconstructionProgress = MutableStateFlow<ReconstructionProgress?>(null)
    val reconstructionProgress: StateFlow<ReconstructionProgress?> =
        _reconstructionProgress.asStateFlow()

    var currentPhotoscanId: Long? = null

    init {
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            cloudRepository.getImageUploadProgress().collect { progress ->
                _imageUploadProgress.value = progress

                // Update state based on upload progress
                if (progress != null) {
                    if (_uploadState.value == UploadState.Idle) {
                        _uploadState.value = UploadState.Uploading
                    }

                    if (progress.uploadedImages == progress.totalImages && progress.totalImages > 0) {
                        _uploadState.value = UploadState.UploadComplete
                    }
                }
            }
        }

        viewModelScope.launch {
            cloudRepository.getReconstructionProgress().collect { progress ->
                _reconstructionProgress.value = progress

                // Update state based on reconstruction progress
                if (progress != null) {
                    when (progress.status) {
                        PhotoscanStatus.STARTED, PhotoscanStatus.RUNNING -> {
                            if (_uploadState.value != UploadState.Reconstructing) {
                                _uploadState.value = UploadState.Reconstructing
                            }
                        }

                        PhotoscanStatus.FINISHED -> {
                            _uploadState.value = UploadState.ReconstructionComplete
                            currentPhotoscanId?.let {
                                cloudRepository.stopReconstructionProgressListener(it)
                            }
                        }

                        PhotoscanStatus.FAILED -> {
                            _uploadState.value = UploadState.Error(
                                progress.message ?: "Reconstruction failed"
                            )
                            currentPhotoscanId?.let {
                                cloudRepository.stopReconstructionProgressListener(it)
                            }
                        }

                        PhotoscanStatus.STOPPED -> {}
                        null -> {}
                    }
                }
            }
        }
    }

    fun start(collectionName: String, autoUpload: Boolean) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading
                cloudRepository.createPhotocollection(
                    name = collectionName,
                    autoUpload = autoUpload
                )
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "Failed to start upload"
                )
            }
        }
    }

    fun startReconstruction() {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.StartingReconstruction

                val photoscanId = cloudRepository.startReconstruction()

                if (photoscanId != null) {
                    currentPhotoscanId = photoscanId
                    cloudRepository.startReconstructionProgressListener(photoscanId)
                    _uploadState.value = UploadState.Reconstructing
                } else {
                    _uploadState.value = UploadState.Error("Failed to start reconstruction")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "Failed to start reconstruction"
                )
            }
        }
    }

    fun uploadRemainingPhotos() = cloudRepository.uploadRemainingPhotos()

    fun reset() {
        currentPhotoscanId?.let {
            cloudRepository.stopReconstructionProgressListener(it)
        }
        currentPhotoscanId = null
        cloudRepository.resetCurrentPhotocollection()
        _uploadState.value = UploadState.Idle
        _imageUploadProgress.value = null
        _reconstructionProgress.value = null
    }

    override fun onCleared() {
        super.onCleared()
        currentPhotoscanId?.let {
            cloudRepository.stopReconstructionProgressListener(it)
        }
    }
}
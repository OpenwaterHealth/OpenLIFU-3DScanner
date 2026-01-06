package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.dto.Coordinates
import health.openwater.openlifu3dscanner.api.dto.ImageCoordinates
import health.openwater.openlifu3dscanner.api.dto.PhotoscanStatus
import health.openwater.openlifu3dscanner.api.model.ImageUploadProgress
import health.openwater.openlifu3dscanner.api.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import health.openwater.openlifu3dscanner.core.CaptureData
import health.openwater.openlifu3dscanner.core.UploadState
import health.openwater.openlifu3dscanner.utils.PositionGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
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

    private var currentPhotoscanId: Long? = null

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

                        savePositionsJson()
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


    private fun savePositionsJson() {
        val collectionName = cloudRepository.getCurrentPhotocollection()?.name ?: return
        val scanDir = File(application.getExternalFilesDir(null), collectionName)

        val jsonFiles = scanDir
            .listFiles { f -> f.extension.equals("json", true) }
            ?.sortedBy { it.name }
            ?: emptyList()

        val captures = jsonFiles.mapNotNull {
            try {
                Gson().fromJson(
                    it.readText(),
                    CaptureData::class.java
                )
            } catch (e: JsonSyntaxException) {
                null
            }
        }

        val positions = PositionGenerator.generatePositions(forwards = captures.map {
            floatArrayOf(
                it.forwardX,
                it.forwardY,
                it.forwardZ
            )
        })
        val coordinatesList = mutableListOf<ImageCoordinates>()

        captures.forEachIndexed { ind, capture ->
            coordinatesList.add(
                ImageCoordinates(
                    image = capture.filename,
                    x = positions[ind][0],
                    y = positions[ind][1],
                    z = positions[ind][2]
                )
            )
        }

        val coordinates = Coordinates(images = coordinatesList)
        cloudRepository.uploadCoordinates(coordinates)
    }

    fun start(collectionId: String) {
        if (!cloudRepository.isLoggedInAndOnline()) {
            _uploadState.value = UploadState.Error("Not logged in or offline")
            return
        }

        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Uploading
                cloudRepository.createPhotocollection(collectionId, autoUpload = true)
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

    fun uploadRemainingPhotos() {
        cloudRepository.uploadRemainingPhotos()
    }

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
package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.App
import health.openwater.openlifu3dscanner.api.dto.Coordinates
import health.openwater.openlifu3dscanner.api.dto.ImageCoordinates
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import health.openwater.openlifu3dscanner.data.FaceDetectionResult
import health.openwater.openlifu3dscanner.data.ImageOrientationData
import health.openwater.openlifu3dscanner.data.OrientationData
import health.openwater.openlifu3dscanner.utils.CameraManager
import health.openwater.openlifu3dscanner.utils.PositionGenerator
import health.openwater.openlifu3dscanner.utils.writeToFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class PhotoCaptureViewModel
@Inject constructor(
    application: Application,
    private val cloudRepository: CloudRepository
): AndroidViewModel(application) {

    private val facesFlow = MutableStateFlow<FaceDetectionResult?>(null)
    private val faceDetectedFlow = MutableStateFlow(false)
    private val faceDetectionCompleteFlow = MutableStateFlow(false)
    private val captureCompleteFlow = MutableStateFlow(false)
    private val capturedImagesNumberFlow = MutableStateFlow(0)

    private var faceDetectionCompleteTimeoutJob: Job? = null
    private var captureJob: Job? = null
    private var referenceNumber: String = ""
    private var photoDir: File? = null
    private var cameraManagerRef = WeakReference<CameraManager>(null)
    private var photoNumber = 0
    private var latestOrientation: OrientationData? = null
    private val orientations = mutableListOf<ImageOrientationData>()

    private val audioManager = getApplication<App>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sound = MediaActionSound()

    fun getFacesFlow(): Flow<FaceDetectionResult?> = facesFlow
    fun getFaceDetectedFlow(): Flow<Boolean> = faceDetectedFlow
    fun getFaceDetectionCompleteFlow(): Flow<Boolean> = faceDetectionCompleteFlow
    fun getCapturedImagesNumberFlow(): Flow<Int> = capturedImagesNumberFlow
    fun getCaptureCompleteFlow(): Flow<Boolean> = captureCompleteFlow

    fun onFacesDetected(result: FaceDetectionResult) {
        facesFlow.value = result

        val detected = result.faces.isNotEmpty()

        if (faceDetectedFlow.value != detected) {
            faceDetectedFlow.value = detected

            if (detected) {
                faceDetectionCompleteTimeoutJob?.cancel()
                faceDetectionCompleteTimeoutJob = viewModelScope.launch {
                    var timeout = 3000
                    while (isActive && timeout > 0) {
                        delay(100)
                        timeout -= 100
                    }
                    if (timeout == 0) {
                        faceDetectionCompleteFlow.value = true
                    }
                }
            } else {
                faceDetectionCompleteFlow.value = false
                faceDetectionCompleteTimeoutJob?.cancel()
                faceDetectionCompleteTimeoutJob = null
            }
        }
    }

    fun setCameraManager(cameraManager: CameraManager) {
        cameraManagerRef = WeakReference(cameraManager)
    }

    fun setReferenceNumber(referenceNumber: String) {
        this.referenceNumber = referenceNumber
        val rootDir = File(Environment.getExternalStorageDirectory(), CloudRepository.OPENLIFU_DIR)
        photoDir = File(rootDir, referenceNumber)
        photoDir?.mkdirs()
    }

    fun getCapturedImageCount(): Int {
        return photoNumber
    }

    fun getTotalImageCount(): Int {
        return TOTAL_IMAGES
    }

    fun startCapture() {
        stopCapture(false)
        captureJob = viewModelScope.launch {
            while (isActive && photoNumber < TOTAL_IMAGES) {
                val fileName = "${referenceNumber}_${String.format("%03d", ++photoNumber)}.jpg"
                val file = File(photoDir, fileName)

                cameraManagerRef.get()?.takePicture(file) {
                    Log.i(TAG, "Photo saved to: $file")
                    latestOrientation?.let {
                        Log.d(TAG, "Orientation: $fileName ${it.forward.asList()}")
                        orientations.add(ImageOrientationData(fileName, it))
                    }

                    capturedImagesNumberFlow.value = photoNumber
                    cloudRepository.onImageCaptured()
                    playShutterClick()
                    scanMediaGallery(file)

                    if (photoNumber == TOTAL_IMAGES) {
                        stopCapture(true)
                        captureCompleteFlow.value = true
                    }
                }

                delay(CAPTURE_DELAY)
            }
        }
    }

    fun stopCapture(complete: Boolean) {
        captureJob?.cancel()
        captureJob = null
        if (complete) {
            savePositionsJson()
        }
    }

    fun onOrientationData(data: OrientationData) {
        latestOrientation = data
    }

    private fun savePositionsJson() {
        if (orientations.isEmpty()) return

        val positions = PositionGenerator.generatePositions(forwards = orientations.map { it.orientation.forward })

        val coordinatesList = mutableListOf<ImageCoordinates>()

        orientations.forEachIndexed { idx, data ->
            val position = positions[idx]

            val imageCoordinates = ImageCoordinates(
                image = data.filename,
                x = position[0],
                y = position[1],
                z = position[2]
            )
            coordinatesList.add(imageCoordinates)

            Log.d(TAG, "Position: forward = ${data.orientation.forward.asList()}, coordinates = $imageCoordinates")
        }

        val coordinates = Coordinates(images = coordinatesList)
        photoDir?.let {
            coordinates.writeToFile(it)
        }

        cloudRepository.uploadCoordinates(coordinates)
    }

    private fun playShutterClick() {
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        if (volume > 0) {
            sound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    private fun scanMediaGallery(savedFile: File) {
        val path = savedFile.absolutePath
        MediaScannerConnection.scanFile(
            getApplication(),
            arrayOf(path),
            arrayOf("image/jpeg"),
            null
        )
    }

    companion object {
        private val TAG = PhotoCaptureViewModel::class.simpleName
        private const val TOTAL_IMAGES = 120
        private const val CAPTURE_DELAY = 1000L
    }

}
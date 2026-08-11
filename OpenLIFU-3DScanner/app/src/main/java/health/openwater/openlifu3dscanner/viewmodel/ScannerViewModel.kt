package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.core.FaceAnalysisResult
import health.openwater.openlifu3dscanner.repository.CloudRepository
import health.openwater.openlifu3dscanner.repository.ScanOwnershipRepository
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.preferences.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val application: Application,
    private val cloudRepository: CloudRepository,
    private val scanOwnershipRepository: ScanOwnershipRepository
) : AndroidViewModel(application) {

    val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    var currentAngle by mutableFloatStateOf(0f)
        private set

    var startingAngle by mutableFloatStateOf(0f)
        private set

    // Pitch is used to avoid unreliable heading measurements near straight up/down.
    // Roll remains available for the level indicator but does not restrict capture.
    var currentPitch by mutableFloatStateOf(0f)
        private set
    var currentRoll by mutableFloatStateOf(0f)
        private set

    // After coordinate remap, pitch is ~0° when the phone is upright. Exclude only
    // the final 10° near straight up/down, where horizontal heading is unstable.
    val isOrientationValid: Boolean
        get() = currentPitch in -80f..80f

    val totalBuckets = Prefs.getPhotoCount(application)
    val captureInterval = 360 / totalBuckets.toFloat()
    val capturedBuckets = mutableStateSetOf<Int>()

    var faceStatus by mutableStateOf(FaceStatus.NO_FACE)
        private set

    val faceDetected: Boolean
        get() = faceStatus == FaceStatus.READY

    private var latestFaceResult by mutableStateOf(FaceAnalysisResult(detected = false))

    // Oval bounds as normalized fractions (0..1)
    private var ovalLeft = 0f
    private var ovalTop = 0f
    private var ovalRight = 1f
    private var ovalBottom = 1f
    private var ovalWidthNorm = 1f

    var currentScanPath by mutableStateOf<File?>(null)
        private set

    // ---- Camera & sensors ----
    lateinit var imageCapture: ImageCapture
    lateinit var cameraExecutor: ExecutorService

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null

    // Prevent concurrent captures
    private var isCapturing by mutableStateOf(false)

    fun initializeCameraAndSensors(isOnline: Boolean) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val captureMode = Prefs.getCaptureMode(application, isOnline)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(captureMode)
            .build()

        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        sensorListener = object : SensorEventListener {

            private val rotationMatrix = FloatArray(9)
            private val remappedMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // Compass heading from camera direction, independent of phone tilt.
                // Device -Z (camera) in world coords: (-R[2], -R[5], -R[8])
                // Project onto horizontal plane (East/North) for bearing.
                val heading = Math.toDegrees(
                    kotlin.math.atan2(
                        -rotationMatrix[2].toDouble(),
                        -rotationMatrix[5].toDouble()
                    )
                ).toFloat()
                currentAngle = if (heading < 0) heading + 360f else heading

                // Pitch for heading validation and roll for the level indicator
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedMatrix
                )
                SensorManager.getOrientation(remappedMatrix, orientation)
                currentPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                currentRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }


        sensorManager?.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun release() {
        sensorManager?.unregisterListener(sensorListener)
        cameraExecutor.shutdown()
    }

    // ---- Scanning control ----
    fun startScanning(collectionName: String) {
        currentScanPath?.deleteRecursively()
        currentScanPath = null

        val file = File(getModelsDir(application), collectionName).apply { mkdirs() }
        currentScanPath = file

        viewModelScope.launch { scanOwnershipRepository.recordOwnership(collectionName) }

        startingAngle = currentAngle  // Store the starting orientation
        _isScanning.value = true
        capturedBuckets.clear()
        _isCompleted.value = false
    }

    fun stopScanning() {
        _isScanning.value = false
    }

    fun resetForRecapture(collectionName: String) {
        currentScanPath?.deleteRecursively()
        currentScanPath = null
        val file = File(getModelsDir(application), collectionName).apply { mkdirs() }
        currentScanPath = file
        capturedBuckets.clear()
        _isCompleted.value = false
        _isScanning.value = false
    }

    // ---- Angle bucket logic ----
    fun angleToBucket(angle: Float): Int {
        // Calculate relative angle from starting position
        var relativeAngle = angle - startingAngle
        if (relativeAngle < 0) relativeAngle += 360f
        return ((relativeAngle % 360f) / captureInterval).toInt().coerceIn(0, totalBuckets - 1)
    }

    // Get the current angle relative to starting position (for UI display)
    fun getRelativeAngle(): Float {
        var relativeAngle = currentAngle - startingAngle
        if (relativeAngle < 0) relativeAngle += 360f
        return relativeAngle
    }

    fun shouldCapture(): Boolean {
        if (!_isScanning.value || isCapturing || !isOrientationValid) return false
        val bucket = angleToBucket(currentAngle)
        return bucket !in capturedBuckets
    }

    fun capturePhoto() {
        if (isCapturing) {
            return
        }

        isCapturing = true

        val angleSnapshot = currentAngle
        val bucket = angleToBucket(angleSnapshot)

        val relativeAngle = bucket * captureInterval
        val timestamp = System.currentTimeMillis()
        val filename = "A_${timestamp}_${relativeAngle.toInt()}.jpg"
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(File(currentScanPath, filename)).build()

        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedBuckets.add(bucket)

                    if (capturedBuckets.size == totalBuckets) {
                        _isScanning.value = false
                        _isCompleted.value = true
                        cloudRepository.onScanComplete()
                    }

                    cloudRepository.onImageCaptured()
                    isCapturing = false
                }

                override fun onError(exc: ImageCaptureException) {
                    isCapturing = false
                }
            })
    }

    fun setOvalBounds(widthFactor: Float, heightFactor: Float, topFraction: Float) {
        ovalWidthNorm = widthFactor
        ovalLeft = (1f - widthFactor) / 2f
        ovalRight = ovalLeft + widthFactor
        ovalTop = topFraction
        ovalBottom = topFraction + heightFactor
    }

    fun setFaceResult(result: FaceAnalysisResult) {
        latestFaceResult = result
        if (!result.detected) {
            faceStatus = FaceStatus.NO_FACE
            return
        }

        val inOval = result.centerX in ovalLeft..ovalRight &&
                result.centerY in ovalTop..ovalBottom
        val minWidth = ovalWidthNorm * 0.4f
        val largeEnough = result.widthFraction >= minWidth

        faceStatus = when {
            !inOval -> FaceStatus.CENTER_FACE
            !largeEnough -> FaceStatus.MOVE_CLOSER
            else -> FaceStatus.READY
        }
    }
}

enum class FaceStatus {
    NO_FACE,
    CENTER_FACE,
    MOVE_CLOSER,
    READY
}

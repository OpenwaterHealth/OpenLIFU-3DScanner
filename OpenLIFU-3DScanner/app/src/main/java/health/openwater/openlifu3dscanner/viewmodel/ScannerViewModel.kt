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
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.core.FaceAnalysisResult
import health.openwater.openlifu3dscanner.repository.CloudRepository
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
    private val cloudRepository: CloudRepository
) : AndroidViewModel(application) {

    val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val _isCompleted = MutableStateFlow(false)
    val isCompleted = _isCompleted.asStateFlow()

    var currentAngle by mutableFloatStateOf(0f)
        private set

    var startingAngle by mutableFloatStateOf(0f)
        private set

    // Pitch and roll for orientation validation
    var currentPitch by mutableFloatStateOf(0f)
        private set
    var currentRoll by mutableFloatStateOf(0f)
        private set

    // Phone must be held roughly upright to capture
    // Pitch ~-90° when vertical; allow ±20° from vertical
    // Roll ~0° when not tilted sideways; allow ±30°
    val isOrientationValid: Boolean
        get() = currentPitch in -110f..-70f && currentRoll in -30f..30f

    val totalBuckets = Prefs.getPhotoCount(application)
    val captureInterval = 360 / totalBuckets.toFloat()
    val capturedBuckets = mutableStateSetOf<Int>()

    val _capturedBucketsCount = MutableStateFlow(0)
    val capturedBucketsCount = _capturedBucketsCount.asStateFlow()


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

    fun initializeCameraAndSensors() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val captureMode = Prefs.getCaptureMode(application)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(captureMode)
            .build()

        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorListener = object : SensorEventListener {

            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuth < 0) azimuth += 360f
                currentAngle = azimuth
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

        val file = File(getModelsDir(), collectionName).apply { mkdirs() }
        currentScanPath = file

        startingAngle = currentAngle  // Store the starting orientation
        _isScanning.value = true
        capturedBuckets.clear()
        _capturedBucketsCount.value = 0
        _isCompleted.value = false
    }

    fun stopScanning() {
        _isScanning.value = false
    }

    fun resetForRecapture(collectionName: String) {
        currentScanPath?.deleteRecursively()
        currentScanPath = null
        val file = File(getModelsDir(), collectionName).apply { mkdirs() }
        currentScanPath = file
        capturedBuckets.clear()
        _capturedBucketsCount.value = 0
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
                    _capturedBucketsCount.value = capturedBuckets.size

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

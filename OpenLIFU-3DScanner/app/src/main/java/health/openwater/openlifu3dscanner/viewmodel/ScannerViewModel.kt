package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import health.openwater.openlifu3dscanner.preferences.Prefs
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val application: Application,
    private val cloudRepository: CloudRepository
) : AndroidViewModel(application) {

    // ---- Scanner state ----
    var isScanning by mutableStateOf(false)
        private set
    var currentAngle by mutableFloatStateOf(0f)
        private set

    val totalBuckets = Prefs.getPhotoCount(application)
    val captureInterval = 360 / totalBuckets.toFloat()
    val capturedBuckets = mutableStateSetOf<Int>()

    var faceDetected by mutableStateOf(false)
        private set

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
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(90)
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

        isScanning = true
        capturedBuckets.clear()
    }

    fun stopScanning(onComplete: () -> Unit) {
        isScanning = false
        onComplete()
    }

    // ---- Angle bucket logic ----
    fun angleToBucket(angle: Float): Int {
        return ((angle % 360f) / captureInterval).toInt().coerceIn(0, totalBuckets - 1)
    }

    fun shouldCapture(): Boolean {
        if (!isScanning || isCapturing) return false
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
                    cloudRepository.onImageCaptured()
                    isCapturing = false
                }

                override fun onError(exc: ImageCaptureException) {
                    isCapturing = false
                }
            })
    }

    fun setFace(face: Boolean) {
        faceDetected = face
    }
}

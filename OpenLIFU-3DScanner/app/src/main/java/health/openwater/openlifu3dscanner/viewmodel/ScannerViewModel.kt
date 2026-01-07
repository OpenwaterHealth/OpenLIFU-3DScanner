package health.openwater.openlifu3dscanner.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import health.openwater.openlifu3dscanner.core.CaptureData
import health.openwater.openlifu3dscanner.core.FaceInfo
import health.openwater.openlifu3dscanner.extensions.getModelsDir
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val application: Application,
) : AndroidViewModel(application) {

    // ---- Scanner state ----
    var isScanning by mutableStateOf(false)
        private set
    var currentAngle by mutableFloatStateOf(0f)
        private set
//    var forward by mutableStateOf(floatArrayOf(0f, 0f, 0f))
//        private set

    val captureInterval = 3f
    val totalBuckets = (360f / captureInterval).toInt()
    val capturedBuckets = mutableStateSetOf<Int>()
    val captureHistory = mutableStateListOf<CaptureData>()

    var faceDetected by mutableStateOf(false)
        private set
    var faceInfo by mutableStateOf<FaceInfo?>(null)
        private set

    var currentScanPath by mutableStateOf<File?>(null)
        private set

    // ---- Camera & sensors ----
    lateinit var imageCapture: ImageCapture
    lateinit var cameraExecutor: ExecutorService

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var sensorListener: SensorEventListener? = null

    var latestOrientation = PhotoOrientation(0f, 0f, 0f, 0L)

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

                val azimuthRad = orientation[0]   // yaw
                val pitchRad = orientation[1]
                val rollRad = orientation[2]

                latestOrientation = PhotoOrientation(
                    azimuthRad = azimuthRad,
                    pitchRad = pitchRad,
                    rollRad = rollRad,
                    timestampNs = event.timestamp
                )
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

        val file = File(application.getModelsDir(), collectionName).apply { mkdirs() }
        currentScanPath = file

        isScanning = true
        capturedBuckets.clear()
        captureHistory.clear()
    }

    fun stopScanning(onComplete: () -> Unit) {
        isScanning = false

        currentScanPath?.let { path ->
            val summaryFile = File(path, "summary.json")
            summaryFile.writeText(Gson().toJson(captureHistory.toList()))
        }

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

    // ---- Capture photo with auto-focus ----
    fun capturePhoto() {
        // Prevent concurrent captures
        if (isCapturing) {
            return
        }

        isCapturing = true

        val angleSnapshot = currentAngle
        val orientationSnapshot = latestOrientation.copy()
        val bucket = angleToBucket(angleSnapshot)

        val relativeAngle = bucket * captureInterval
        val timestamp = System.currentTimeMillis()
        val filename = "scan_${timestamp}_A${relativeAngle.toInt()}.jpg"
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(File(currentScanPath, filename)).build()

        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val captureData = CaptureData(
                        timestamp = timestamp,
                        angle = relativeAngle,
                        absoluteAngle = angleSnapshot,
                        filename = filename,
                        azimuthRad = orientationSnapshot.azimuthRad,
                        pitchRad = orientationSnapshot.pitchRad,
                        rollRad = orientationSnapshot.rollRad
                    )

                    capturedBuckets.add(bucket)
                    captureHistory.add(captureData)

                    val metadataFile = File(currentScanPath, filename.replace(".jpg", ".json"))
                    metadataFile.writeText(Gson().toJson(captureData))

                    isCapturing = false
                }

                override fun onError(exc: ImageCaptureException) {
                    isCapturing = false
                }
            })
    }

    fun setFace(face: FaceInfo?) {
        faceInfo = face
        faceDetected = face != null
    }
}

data class PhotoOrientation(
    val azimuthRad: Float,
    val pitchRad: Float,
    val rollRad: Float,
    val timestampNs: Long
)

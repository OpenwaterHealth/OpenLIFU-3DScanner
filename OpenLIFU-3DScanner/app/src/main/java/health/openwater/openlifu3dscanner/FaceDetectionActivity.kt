package health.openwater.openlifu3dscanner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class FaceDetectionActivity : BaseActivity() {

    private lateinit var faceStatusTextView: TextView
    private lateinit var previewView: PreviewView
    private var noseCoordinates: Triple<Float, Float, Float>? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null // Added to manage camera
    private var referenceNumber: String = "DEFAULT_REF" // Default reference number

    override fun onCreate(savedInstanceState: Bundle?) {
        //LogFileUtil.appendLog("Face detection screen started")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_detection)

        // Initialize views
        previewView = findViewById(R.id.previewView)
        faceStatusTextView = findViewById(R.id.faceStatusTextView)

        // Get the reference number from the previous activity
        referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "DEFAULT_REF"

        cameraExecutor = Executors.newSingleThreadExecutor()
        //LogFileUtil.appendLog("Face detection starting...")
        // Start face detection
        detectFace()
        //LogFileUtil.appendLog("Face detection started")
    }

    private fun detectFace() {
        //LogFileUtil.appendLog("Detecting Face...")
        faceStatusTextView.text = "Detecting Face..."
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get() // Save the cameraProvider instance

            // Set up the Preview use case
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Set up the ImageAnalysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Set the analyzer
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageForFaceDetection(imageProxy)
            }

            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind any previous use cases and bind the new ones
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                //LogFileUtil.appendLog("Error initializing camera: ${exc.message}")
                Toast.makeText(this, "Error initializing camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageForFaceDetection(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Configure face detection options
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    // Extract the nose landmark position
                    val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position

                    if (nose != null) {
                        //LogFileUtil.appendLog("Face detected")
                        noseCoordinates = Triple(nose.x, nose.y, 0f)

                        faceStatusTextView.text = "Face Detected!"
                        Log.d("NoseDetection", "Nose position: $nose.x, $nose.y")
                        // Stop face detection and transition to AR session
                        releaseCameraResources()
                        transferToMainActivity()
                        break
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                //LogFileUtil.appendLog("Error detecting face: ${it.message}")
                faceStatusTextView.text = "Face Detection Failed. Retrying..."
                Log.e("FaceDetection", "Error detecting face: ${it.message}")
                imageProxy.close()
            }
    }

    private fun releaseCameraResources() {
        //LogFileUtil.appendLog("Releasing camera resources")
        try {
            cameraProvider?.unbindAll() // Unbind all use cases
            //LogFileUtil.appendLog("Camera resources released")
        } catch (e: Exception) {
            //LogFileUtil.appendLog("Error releasing camera resources: ${e.message}")
        }
    }

    private fun transferToMainActivity() {
        val handler = android.os.Handler()
        handler.postDelayed({
            //LogFileUtil.appendLog("Transferring to AR session screen")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("NOSE_COORDINATES", noseCoordinates)
            intent.putExtra("REFERENCE_NUMBER", referenceNumber)
            startActivity(intent)
            finish()
        }, 500)
    }

    override fun onDestroy() {
        super.onDestroy()
        //releaseCameraResources() // Ensure resources are released when activity is destroyed
        cameraExecutor.shutdown()
    }
}

package health.openwater.openlifu3dscanner.utils

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import health.openwater.openlifu3dscanner.data.FaceDetectionResult
import java.io.File

class CameraManager(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val faceDetectionListener: (result: FaceDetectionResult) -> Unit
) {
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    private val cameraExecutor = ContextCompat.getMainExecutor(context)
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var cameraProvider: ProcessCameraProvider

    init {
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            setupCamera()
        }, cameraExecutor)
    }

    fun takePicture(file: File, onImageSaved: () -> Unit) {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    Log.e(TAG, error.message, error)
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onImageSaved()
                }
            })
    }

    fun stopFaceDetection() {
        cameraProvider.unbind(imageAnalyzer)
    }

    private fun createAnalyzer(): ImageAnalysis.Analyzer {
        return FaceDetectionProcessor(previewView, faceDetectionListener)
    }

    private fun setupCamera() {
        val rotation = previewView.display.rotation

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, createAnalyzer())
            }

        val preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.surfaceProvider = previewView.getSurfaceProvider()

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer, imageCapture)
    }

    companion object {
        private val TAG = CameraManager::class.simpleName
    }
}
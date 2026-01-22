package health.openwater.openlifu3dscanner.screen.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import health.openwater.openlifu3dscanner.core.FaceAnalyzer
import health.openwater.openlifu3dscanner.viewmodel.ScannerViewModel


@Composable
fun ScannerComponent(
    onProceed: () -> Unit,
    collectionName: String,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isScanning by viewModel.isScanning.collectAsState()

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        viewModel.initializeCameraAndSensors()
    }

    LaunchedEffect(previewView) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(viewModel.cameraExecutor, FaceAnalyzer { face ->
                    viewModel.setFace(face)
                })
            }

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            viewModel.imageCapture,
            imageAnalyzer
        )
    }

    // Clean up resources when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }

    LaunchedEffect(viewModel.currentAngle, viewModel.faceDetected, viewModel.isScanning) {
        if (viewModel.shouldCapture()) {
            viewModel.capturePhoto()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        ScanControls(
            onProceed = onProceed,
            onStartStop = {
                if (!isScanning) {
                    viewModel.startScanning(collectionName)
                } else {
                    viewModel.stopScanning()
                }
            }
        )
    }
}
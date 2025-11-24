package health.openwater.openlifu3dscanner.utils

import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import health.openwater.openlifu3dscanner.data.FaceDetectionResult

class FaceDetectionProcessor(
    private val previewView: PreviewView,
    private val faceDetectionListener: (result: FaceDetectionResult) -> Unit,
): ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
            detector.process(inputImage)
                .addOnSuccessListener { result ->
                    faceDetectionListener(
                        FaceDetectionResult(
                            getImageToPreviewMatrix(imageProxy, previewView),
                            result
                        )
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, e.message, e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun getImageToPreviewMatrix(imageProxy: ImageProxy, previewView: PreviewView): Matrix {
        val cropRect = imageProxy.cropRect
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val matrix = Matrix()

        // Source vertices: image crop rect in image coords
        val source = floatArrayOf(
            cropRect.left.toFloat(),  cropRect.top.toFloat(),
            cropRect.right.toFloat(), cropRect.top.toFloat(),
            cropRect.right.toFloat(), cropRect.bottom.toFloat(),
            cropRect.left.toFloat(),  cropRect.bottom.toFloat()
        )

        // Destination vertices: full PreviewView rect
        val destination = floatArrayOf(
            0f, 0f,
            previewView.width.toFloat(), 0f,
            previewView.width.toFloat(), previewView.height.toFloat(),
            0f, previewView.height.toFloat()
        )

        // Shift destination vertices depending on rotation
        val vertexSize = 2
        val shiftOffset = (rotationDegrees / 90) * vertexSize
        val temp = destination.clone()
        for (toIndex in source.indices) {
            val fromIndex = (toIndex + shiftOffset) % source.size
            destination[toIndex] = temp[fromIndex]
        }

        matrix.setPolyToPoly(source, 0, destination, 0, 4)
        return matrix
    }

    companion object {
        private val TAG = FaceDetectionProcessor::class.simpleName
    }
}
package health.openwater.openlifu3dscanner.core

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

data class FaceAnalysisResult(
    val detected: Boolean,
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val widthFraction: Float = 0f,
    val heightFraction: Float = 0f
)

class FaceAnalyzer(
    private val onFaceAnalyzed: (FaceAnalysisResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val detector = FaceDetection.getClient(options)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            // Compute rotation-corrected image dimensions
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val imageWidth: Int
            val imageHeight: Int
            if (rotationDegrees == 90 || rotationDegrees == 270) {
                imageWidth = mediaImage.height
                imageHeight = mediaImage.width
            } else {
                imageWidth = mediaImage.width
                imageHeight = mediaImage.height
            }

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val box = faces[0].boundingBox
                        val cx = (box.centerX().toFloat()) / imageWidth
                        val cy = (box.centerY().toFloat()) / imageHeight
                        val wFrac = box.width().toFloat() / imageWidth
                        val hFrac = box.height().toFloat() / imageHeight
                        onFaceAnalyzed(
                            FaceAnalysisResult(
                                detected = true,
                                centerX = cx,
                                centerY = cy,
                                widthFraction = wFrac,
                                heightFraction = hFrac
                            )
                        )
                    } else {
                        onFaceAnalyzed(FaceAnalysisResult(detected = false))
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

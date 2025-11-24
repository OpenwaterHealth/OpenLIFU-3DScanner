package health.openwater.openlifu3dscanner.data

import android.graphics.Matrix
import com.google.mlkit.vision.face.Face

data class FaceDetectionResult(
    val transform: Matrix,
    val faces: List<Face>
)
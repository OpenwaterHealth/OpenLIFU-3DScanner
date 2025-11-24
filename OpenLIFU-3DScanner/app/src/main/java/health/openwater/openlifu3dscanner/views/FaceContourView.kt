package health.openwater.openlifu3dscanner.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import health.openwater.openlifu3dscanner.data.FaceDetectionResult

class FaceContourView(context: Context?, attrs: AttributeSet?): View(context, attrs) {

    private val faceRects = mutableListOf<Rect>()
    private val paint: Paint = Paint()
    private var transform: Matrix? = null

    init {
        paint.apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                3f,
                resources.displayMetrics
            )
            isAntiAlias = true
        }
    }

    fun setFaces(faceDetectionResult: FaceDetectionResult?) {
        if (faceDetectionResult == null) return
        Log.d("###", "FACES: $faceDetectionResult")
        synchronized(faceRects) {
            transform = faceDetectionResult.transform
            faceRects.clear()
            faceDetectionResult.faces.forEach { face ->
                faceRects.add(face.boundingBox)
            }
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.TRANSPARENT)

        val m = transform ?: return

        for (r in faceRects) {
            val rect = RectF(r)
            m.mapRect(rect)
            canvas.drawRect(rect, paint)
        }
    }
}
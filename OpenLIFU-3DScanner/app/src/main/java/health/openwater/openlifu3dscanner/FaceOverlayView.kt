package health.openwater.openlifu3dscanner

import android.content.Context
import android.graphics.*
import android.view.View
import com.google.mlkit.vision.common.Triangle
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.facemesh.FaceMesh
import androidx.core.graphics.toColorInt


class FaceOverlayView(context: Context) : View(context) {
    private var points: List<FaceMeshPoint> = emptyList()
    private var triangles: List<Triangle<FaceMeshPoint>> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1
    private var boundingBox: Rect? = null

    // at top-level in FaceOverlayView class
    private var meshPointColor: Int = Color.RED
    private var meshLineColor: Int = Color.RED

    /** Call this to switch mesh colors (e.g., when centered vs not). */
    fun setMeshDetected(detected: Boolean) {
        // Green tones when centered, grey/red-ish when not
        meshPointColor = if (detected) "#48ff00".toColorInt() else "#f96c34".toColorInt()
        meshLineColor  = if (detected) "#48ff00".toColorInt() else "#f96c34".toColorInt()
        postInvalidateOnAnimation()
    }


    fun updatePoints(
        meshPoints: List<FaceMeshPoint>,
        imageWidth: Int,
        imageHeight: Int,
        boundingBox: Rect?
    ) {
        this.points = meshPoints
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.boundingBox = boundingBox
        invalidate()
    }

    fun updateFaceMesh(faceMesh: FaceMesh, imageWidth: Int, imageHeight: Int) {
        this.points = faceMesh.allPoints
        this.triangles = faceMesh.allTriangles
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.boundingBox = faceMesh.boundingBox
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val faceBox = boundingBox ?: return
        if (points.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        val cx = faceBox.exactCenterX() * scaleX
        val cy = faceBox.exactCenterY() * scaleY
        val factor = 1.4f

        // 1) Precompute screen coords once (by landmark index)
        val screen = HashMap<Int, PointF>(points.size)
        for (p in points) {
            val x = p.position.x * scaleX
            val y = p.position.y * scaleY
            val sx = cx + (x - cx) * factor
            val sy = cy + (y - cy) * factor
            screen[p.index] = PointF(sx, sy) // FaceMeshPoint.index is stable per landmark
        }

        // Paints
        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = meshPointColor
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = meshLineColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
            alpha = 180
        }

        // 2) Draw triangles using the precomputed coords
        if (triangles.isNotEmpty()) {
            val path = Path()
            for (tri in triangles) {
                val ps = tri.allPoints
                if (ps.size >= 3) {
                    val p0 = screen[ps[0].index] ?: continue
                    val p1 = screen[ps[1].index] ?: continue
                    val p2 = screen[ps[2].index] ?: continue
                    path.reset()
                    path.moveTo(p0.x, p0.y)
                    path.lineTo(p1.x, p1.y)
                    path.lineTo(p2.x, p2.y)
                    path.close()
                    canvas.drawPath(path, linePaint)
                }
            }
        }

        // 3) Draw landmark dots once each, reusing the same coords
        for ((_, pt) in screen) {
            canvas.drawCircle(pt.x, pt.y, 3f, pointPaint)
        }

        // Optional: bounding box (already scaled)
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.TRANSPARENT
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val scaledBox = Rect(
            (faceBox.left * scaleX).toInt(),
            (faceBox.top * scaleY).toInt(),
            (faceBox.right * scaleX).toInt(),
            (faceBox.bottom * scaleY).toInt()
        )
        canvas.drawRect(scaledBox, boxPaint)
    }

}
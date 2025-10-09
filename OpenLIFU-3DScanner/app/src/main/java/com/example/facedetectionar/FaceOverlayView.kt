package com.example.facedetectionar

import android.content.Context
import android.graphics.*
import android.view.View
import com.google.mlkit.vision.common.Triangle
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint
class FaceOverlayView(context: Context) : View(context) {

    // ---- toggles ----
    var showPoints: Boolean = true
    var showTriangles: Boolean = true

    // ---- sizes ----
    private var pointRadius = 3f
    fun setPointSize(radius: Float) { pointRadius = radius; invalidate() }

    // ---- ML Kit storage ----
    private var mlPoints: List<FaceMeshPoint> = emptyList()
    private var mlTriangles: List<com.google.mlkit.vision.common.Triangle<FaceMeshPoint>> = emptyList()
    private var mlBoundingBox: Rect? = null

    // ---- MediaPipe storage (pixel-space) ----
    private var mpPointsPx: List<PointF> = emptyList()
    private var mpTrianglesIdx: List<IntArray> = emptyList()   // <— NEW: triples of vertex indices (i0,i1,i2)
    private var mpBoundingBox: Rect? = null

    private var imageWidth = 1
    private var imageHeight = 1

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 1.5f
        style = Paint.Style.FILL
    }
    private val linePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        alpha = 200
    }
    private val boxPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    fun updateFaceMesh(faceMesh: FaceMesh, w: Int, h: Int) {
        // MLKit mode
        mpPointsPx = emptyList()
        mpTrianglesIdx = emptyList()
        mpBoundingBox = null

        mlPoints = faceMesh.allPoints
        mlTriangles = faceMesh.allTriangles
        imageWidth = w
        imageHeight = h
        mlBoundingBox = faceMesh.boundingBox
        invalidate()
    }

    /** MediaPipe mode without triangles (points only) */
    fun updatePointsPx(pointsPx: List<PointF>, w: Int, h: Int, box: Rect? = null) {
        // MP mode
        mlPoints = emptyList()
        mlTriangles = emptyList()
        mlBoundingBox = null

        mpPointsPx = pointsPx
        mpTrianglesIdx = emptyList()      // <— ensure cleared if not supplied
        imageWidth = w
        imageHeight = h
        mpBoundingBox = box ?: computeBoundingBox(pointsPx)
        invalidate()
    }

    /** MediaPipe mode WITH triangle indices (preferred for full mesh) */
    fun updatePointsPx(
        pointsPx: List<PointF>,
        trianglesIdx: List<IntArray> = FaceMeshTriangulation.triangles,     // each = intArrayOf(i0, i1, i2)
        w: Int,
        h: Int,
        box: Rect? = null
    ) {
        // MP mode
        mlPoints = emptyList()
        mlTriangles = emptyList()
        mlBoundingBox = null

        mpPointsPx = pointsPx
        mpTrianglesIdx = trianglesIdx
        imageWidth = w
        imageHeight = h
        mpBoundingBox = box ?: computeBoundingBox(pointsPx)
        invalidate()
    }

    fun clear() {
        mlPoints = emptyList()
        mlTriangles = emptyList()
        mlBoundingBox = null

        mpPointsPx = emptyList()
        mpTrianglesIdx = emptyList()
        mpBoundingBox = null

        imageWidth = 1
        imageHeight = 1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val hasMp = mpPointsPx.isNotEmpty()
        val hasMl = mlPoints.isNotEmpty()
        if (!hasMp && !hasMl) return

        val iw = imageWidth.coerceAtLeast(1)
        val ih = imageHeight.coerceAtLeast(1)

        // === MATCH OLD BEHAVIOR: simple scale to view, no center-crop ===
        val scaleX = width.toFloat() / iw
        val scaleY = height.toFloat() / ih

        if (hasMp) {
            val faceBox = mpBoundingBox ?: computeBoundingBox(mpPointsPx) ?: return
            val cx = faceBox.exactCenterX() * scaleX
            val cy = faceBox.exactCenterY() * scaleY
            val factor = 1.4f  // same as your old overlay

            // --- triangles (wireframe) ---
            if (showTriangles && mpTrianglesIdx.isNotEmpty()) {
                for (tri in mpTrianglesIdx) {
                    if (tri.size < 3) continue
                    val (i0, i1, i2) = tri
                    if (i0 !in mpPointsPx.indices || i1 !in mpPointsPx.indices || i2 !in mpPointsPx.indices) continue

                    var x0 = mpPointsPx[i0].x * scaleX; var y0 = mpPointsPx[i0].y * scaleY
                    var x1 = mpPointsPx[i1].x * scaleX; var y1 = mpPointsPx[i1].y * scaleY
                    var x2 = mpPointsPx[i2].x * scaleX; var y2 = mpPointsPx[i2].y * scaleY

                    x0 = cx + (x0 - cx) * factor; y0 = cy + (y0 - cy) * factor
                    x1 = cx + (x1 - cx) * factor; y1 = cy + (y1 - cy) * factor
                    x2 = cx + (x2 - cx) * factor; y2 = cy + (y2 - cy) * factor

                    canvas.drawLine(x0, y0, x1, y1, linePaint)
                    canvas.drawLine(x1, y1, x2, y2, linePaint)
                    canvas.drawLine(x2, y2, x0, y0, linePaint)
                }
            }

            // --- points ---
            if (showPoints) {
                for (p in mpPointsPx) {
                    var x = p.x * scaleX
                    var y = p.y * scaleY
                    x = cx + (x - cx) * factor
                    y = cy + (y - cy) * factor
                    canvas.drawCircle(x, y, pointRadius, pointPaint)
                }
            }

            // optional box (same style as before)
            val boxPaint = Paint().apply { color = Color.TRANSPARENT; strokeWidth = 3f; style = Paint.Style.STROKE }
            val scaledBox = Rect(
                (faceBox.left * scaleX).toInt(),
                (faceBox.top * scaleY).toInt(),
                (faceBox.right * scaleX).toInt(),
                (faceBox.bottom * scaleY).toInt()
            )
            canvas.drawRect(scaledBox, boxPaint)
            return
        }

        // === ML Kit branch stays the same as your old overlay ===
        // (triangles = faceMesh.allTriangles, points = faceMesh.allPoints with same factor=1.4f)
        // ... keep your existing ML Kit code path here ...
    }


    private fun computeBoundingBox(points: List<PointF>): Rect? {
        if (points.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        if (!minX.isFinite() || !minY.isFinite() || !maxX.isFinite() || !maxY.isFinite()) return null
        return Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt())
    }
}


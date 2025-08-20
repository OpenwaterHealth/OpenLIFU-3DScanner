package com.example.facedetectionar

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.View
import com.google.mlkit.vision.common.Triangle
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.google.mlkit.vision.facemesh.FaceMesh


class FaceOverlayView(context: Context) : View(context) {
    private var points: List<FaceMeshPoint> = emptyList()
    private var triangles: List<Triangle<FaceMeshPoint>> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1
    private var boundingBox: Rect? = null

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
        if (points.isEmpty() || boundingBox == null) return

        val faceBox = boundingBox!!
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        // Create paints for points and lines
        val pointPaint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 2f
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
            alpha = 180 // Semi-transparent
        }

        // Draw mesh triangles (connections)
        if (triangles.isNotEmpty()) {
            for (triangle in triangles) {
                val connectedPoints = triangle.allPoints
                if (connectedPoints.size >= 3) {
                    val path = Path()

                    for (i in connectedPoints.indices) {
                        val point = connectedPoints[i]
                        var x = point.position.x * scaleX
                        var y = point.position.y * scaleY

                        // Apply scaling factor around face center
                        val cx = (faceBox.exactCenterX()) * scaleX
                        val cy = (faceBox.exactCenterY()) * scaleY
                        val factor = 1.4f

                        x = cx + (x - cx) * factor
                        y = cy + (y - cy) * factor

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    path.close()
                    canvas.drawPath(path, linePaint)
                }
            }
        }

        // Draw points
        for (point in points) {
            var x = point.position.x * scaleX
            var y = point.position.y * scaleY

            // Apply scaling factor around face center
            val cx = (faceBox.exactCenterX()) * scaleX
            val cy = (faceBox.exactCenterY()) * scaleY
            val factor = 1.4f

            x = cx + (x - cx) * factor
            y = cy + (y - cy) * factor

            canvas.drawCircle(x, y, 3f, pointPaint)
        }

        // Optional: Draw bounding box
        val boxPaint = Paint().apply {
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
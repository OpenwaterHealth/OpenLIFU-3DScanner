package com.example.facedetectionar

import android.content.Context
import android.graphics.*
import android.view.View
import com.google.mlkit.vision.facemesh.FaceMeshPoint

class FaceOverlayView(context: Context) : View(context) {
    private var points: List<FaceMeshPoint> = emptyList()
    private var imageWidth = 1
    private var imageHeight = 1

    fun updatePoints(meshPoints: List<FaceMeshPoint>, imageWidth: Int, imageHeight: Int) {
        this.points = meshPoints
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        val paint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 6f
            style = Paint.Style.FILL
        }

        for (point in points) {
            val x = point.position.x * scaleX
            val y = point.position.y * scaleY
            canvas.drawCircle(x, y, 3f, paint)
        }
    }
}

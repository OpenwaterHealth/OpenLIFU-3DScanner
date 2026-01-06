package health.openwater.openlifu3dscanner.screen.photoscan

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.ScaleGestureDetector

@SuppressLint("ViewConstructor")
class ModelSurfaceView(
    context: Context,
    modelDir: String
) : GLSurfaceView(context) {

    private val renderer = ModelRenderer(modelDir)
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    private var prevX = 0f
    private var prevY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        if (scaleDetector.isInProgress) {
            return true
        }

        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
            }

            android.view.MotionEvent.ACTION_MOVE -> {
                val dx = event.x - prevX
                val dy = event.y - prevY

                renderer.rotate(dx * 0.2f, dy * 0.2f)
                requestRender()

                prevX = event.x
                prevY = event.y
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.zoom(detector.scaleFactor)
            requestRender()
            return true
        }
    }
}
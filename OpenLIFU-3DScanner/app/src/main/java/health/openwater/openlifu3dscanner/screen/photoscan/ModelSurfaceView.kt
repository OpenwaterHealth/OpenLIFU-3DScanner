package health.openwater.openlifu3dscanner.screen.photoscan

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector

@SuppressLint("ViewConstructor")
class ModelSurfaceView(
    context: Context,
    modelDir: String,
    onReady: (() -> Unit)? = null
) : GLSurfaceView(context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val renderer = ModelRenderer(modelDir) {
        mainHandler.post { onReady?.invoke() }
    }
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    private var prevX = 0f
    private var prevY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = event.x - prevX
                    val dy = event.y - prevY

                    renderer.rotate(dx * 0.2f, dy * 0.2f)
                    requestRender()

                    prevX = event.x
                    prevY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // If a finger is lifted during a pinch, reset prevX/Y to remaining finger
                if (event.pointerCount > 1) {
                    // pick the remaining finger (usually index 0 if lifted is 1)
                    val newIndex = if (event.actionIndex == 0) 1 else 0
                    prevX = event.getX(newIndex)
                    prevY = event.getY(newIndex)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Reset
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
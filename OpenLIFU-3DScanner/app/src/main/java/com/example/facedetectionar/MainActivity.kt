package com.example.facedetectionar


import android.Manifest
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.util.SparseIntArray
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.example.facedetectionar.Modals.bulletPointConfig
import com.google.android.filament.Colors
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableException
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.material.setColor
import io.github.sceneview.math.Position
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt
import com.example.facedetectionar.api.repository.ReconstructionRepository
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mlkit.vision.facemesh.FaceMesh
// MediaPipe Tasks
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import dagger.hilt.android.AndroidEntryPoint
import io.github.sceneview.math.Direction
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var reconstructionRepository: ReconstructionRepository

    private companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        const val EXTRA_ALREADY_RESET = "EXTRA_ALREADY_RESET"

    }

    // --- MediaPipe Face Landmarker ---
    private lateinit var mpFaceLandmarker: FaceLandmarker
    private var mpReady = false
    private var lastMpLandmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> = emptyList()

    private lateinit var angleHud: TextView
    private var showAngleHud = true
    private lateinit var pivotNode: Node;
    // Tracks the pivot's current local transform (relative to its AnchorNode)
    private var angleLogFrame = 0

    private var pivotBaseQ = Quaternion(0f, 0f, 0f, 1f)

    private var userYawDeg: Float = 0f                 // extra yaw you control
    private var userYawInHeadSpace: Boolean = true     // rotate about head's up axis

    private var angleDegG: Double? =null
    private var yawLRDegG= 0.0F
    private var pitchUDegG: Double? =null


    private var cubeNode: ModelNode? = null
    private lateinit var sceneView: ARSceneView
    private lateinit var anchorNode: AnchorNode;

    private lateinit var latestNosePosition: Point3D
    private lateinit var latestCamPosition: Point3D

    private var zDistance=0.0f;

    val capturedModelList = mutableSetOf<Node>()
    val nonCapturedModelList = mutableListOf<ModelNode>()
    private val faceCircleUri = "models/circle.glb"
    private val faceCubeUri = "models/cube.glb"
    private var activeRing:ModelNode?=null
    private var previousPosition: Vector3? = null
    private var previousTime: Long = 0
    private var lastToastTime: Long = 0
    private val toastCooldown = 2000L
    private var maxAllowedSpeed = 0.6f
    private var delayCaptureBy = 1000
    private lateinit var startButton: Button
    private lateinit var confirmButton: Button
    private lateinit var BackInStartCapture: Button
    private val capturedDataList = mutableListOf<Map<String, Any>>()
    private var currentRingIndex = 0
    private var referenceNumber: String = "DEFAULT_REF"
    private var imageCounter: Int = 1
    private var IsCaptureStarted: Boolean = true
    private var IsTrackingStarted: Boolean = false
    private lateinit var distanceLabel: TextView
    private lateinit var faceOverlayView: FaceOverlayView

    private val arrowList = mutableListOf<bulletPointConfig>()
    private lateinit var sensorManager: SensorManager
    private lateinit var rotationVectorSensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var angleString = 0
    private var adjustedZ = 0.0f
    private lateinit var minAngleText: TextView
    private lateinit var maxAngleText: TextView
    private lateinit var faceRing: ImageView
    private var frameCounter = 0
    private var isFaceDetected = false
    private var hasResetForCurrentFace = false

    private var lastFaceDetectTime = 0L


    private  val ROT_FIX_X_DEG = 0f      // try 0 / ±90
    private  val ROT_FIX_Y_DEG = 0f      // if you see a corner, try ±45 or 180
    private  val ROT_FIX_Z_DEG = 0f

    private lateinit var angleFilter:AngleNoiseFilter




    private var mesh1 : FaceMesh?=null

    private var emaDist: Float? = null
    private val DIST_ALPHA = 0.25f  // 0..1 (higher = snappier)

    /** Simple EMA smoothing for a Float */
    private fun ema(prev: Float?, cur: Float, a: Float): Float =
        if (prev == null) cur else prev + a * (cur - prev)

    data class CameraConfig(
        val minDistance: Float,
        val maxDistance: Float
    )

    data class CubeConfig(
        val cubeSize: Float,
    )

    data class Point3D(val x: Double, val y: Double, val z: Double)

    data class FaceRingConfig(
        val initialRingSize: Float,
        val ringScaleFactor: Float,
    )

    data class CameraRingConfig(
        val ringSize: Int,

        )


    data class Bucket(val start: Float, val end: Float, val value: Float)

    private val BUCKETS = listOf(
        Bucket(-70f, -68f, -0.53133946f),
        Bucket(-65f, -63f, -0.55133946f),
        Bucket(-60f, -58f, -0.60133946f),
        Bucket(-55f, -53f, -0.63133946f),
        Bucket(-50f, -48f, -0.65133946f),
        Bucket(-45f, -43f, -0.68133946f),
        Bucket(-37f, -35f, -0.74133946f),
    )


    private lateinit var cameraConfigCapture: CameraConfig
    private lateinit var cameraConfigDetection: CameraConfig
    private lateinit var cubeConfig: CubeConfig
    private lateinit var faceRingConfig: FaceRingConfig
    private lateinit var cameraRingConfig: CameraRingConfig
    private var isCaptureInProgress = false



    private lateinit var loadingOverlay: View
    private lateinit var loadingMessage: TextView
    private var captureTimer: CountDownTimer? = null
    private var initialCameraPose: Pose? = null

    private lateinit var faceThread: android.os.HandlerThread
    private lateinit var faceHandler: android.os.Handler
    @Volatile private var isProcessingFrame = false
    private val TARGET_FPS = 10
    private val MIN_INTERVAL_NS = 1_000_000_000L / TARGET_FPS // 33,333,333 ns
    private var lastDispatchTimestampNs = 0L


    private fun showLoading(message: String = "Initializing...") {
        runOnUiThread {
            loadingMessage.text = message
            loadingOverlay.visibility = View.VISIBLE
        }
    }

    private fun hideLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
        }
    }

    private fun getFaceDistanceMeters(): Pair<Float, Float>? {
        val frame = sceneView.session?.frame ?: return null
        val cam = frame.camera
        if (lastMpLandmarks.isEmpty()) return null

        return try {
            // Build PnP inputs
            val objPts = model3dMat()
            val imgPts = mp2dToPixels(lastMpLandmarks, cam)
            val K = cameraMatrixFromAR(cam)
            val dist = MatOfDouble(Mat.zeros(1, 5, org.opencv.core.CvType.CV_64F))

            // Solve head -> camera pose
            val rvec = Mat()
            val tvec = Mat()
            org.opencv.calib3d.Calib3d.solvePnP(
                objPts, imgPts, K, dist, rvec, tvec, false,
                org.opencv.calib3d.Calib3d.SOLVEPNP_ITERATIVE
            )

            // OpenCV camera coords (X right, Y down, Z forward in meters)
            val tx = tvec.get(0, 0)[0]
            val ty = tvec.get(1, 0)[0]
            val tz = tvec.get(2, 0)[0]

            val total = kotlin.math.sqrt(tx*tx + ty*ty + tz*tz).toFloat()  // Euclidean distance
            val depth = tz.toFloat()                                       // forward along camera axis

            // Optional smoothing
            val smooth = ema(emaDist, total, DIST_ALPHA).also { emaDist = it }
            Pair(smooth, depth)

        } catch (e: Exception) {
            android.util.Log.e("FaceDistance", "Failed: ${e.message}")
            null
        }
    }

    private fun setupAngleHud() {
        angleHud = TextView(this).apply {
            text = "—"
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(android.graphics.Color.WHITE)
            setPadding(12, 8, 12, 8)
            setBackgroundColor(0x66000000) // semi-transparent black
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = 16
            topMargin = 16
        }
        addContentView(angleHud, lp)
    }

    private fun setConfirmEnabled(enabled: Boolean) = runOnUiThread {
        if (confirmButton.isEnabled != enabled) confirmButton.isEnabled = enabled
    }

    private fun updateAngleHud(yaw: Float) {
        if (!showAngleHud) return
        val s = String.format(Locale.US, "Shift Angle %.1f°",yaw)
        runOnUiThread {
            angleHud.text = s
            angleHud.visibility = android.view.View.VISIBLE
        }
    }
    private fun deg(d: Float) = Math.toRadians(d.toDouble()).toFloat()

    private fun modelCorrectionQuat(): dev.romainguy.kotlin.math.Quaternion =
        createQuaternionFromAxisAngle(Vector3(1f,0f,0f), deg(ROT_FIX_X_DEG)) *
                createQuaternionFromAxisAngle(Vector3(0f,1f,0f), deg(ROT_FIX_Y_DEG)) *
                createQuaternionFromAxisAngle(Vector3(0f,0f,1f), deg(ROT_FIX_Z_DEG))

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            // Load arrow data from JSON

            setupAngleHud()

            angleFilter = AngleNoiseFilter(
                windowSize =  9,   // try 7–11 for your stream
                deadband   = 0.5f,            // ignore micro-jitter within ±0.2°
                emaAlpha   = 0.29f,           // higher = more responsive
                maxDegPerSec = 300f,          // raise for snappier tracking
                fpsHint    = 30f              // your processing FPS
            )

            faceThread = android.os.HandlerThread("FaceDetectThread")
            faceThread.start()
            faceHandler = android.os.Handler(faceThread.looper)

            setupMediaPipeLandmarker()
            Log.i("OPENCV","LINE0")

            loadsDataFromJson()

            loadsCameraConfigFromJson();





            //initialize session
            initializeARScene()


            val alreadyReset = intent.getBooleanExtra(EXTRA_ALREADY_RESET, false)

            // If we already reset once, mark face as detected
            if (alreadyReset) {
                hasResetForCurrentFace = true
            }




            // Initialize UI components
            val overlayContainer = findViewById<FrameLayout>(R.id.overlayContainer)
            faceOverlayView = FaceOverlayView(this)
            overlayContainer.addView(faceOverlayView)
            distanceLabel = findViewById(R.id.distanceLabel)
            sceneView = findViewById(R.id.arSceneView)
            BackInStartCapture = findViewById<Button>(R.id.BackInStartCapture)
            startButton = findViewById(R.id.startButton)
            confirmButton = findViewById<Button>(R.id.confirmButtonInMain)
            loadingOverlay = findViewById(R.id.loadingOverlay)
            loadingMessage = findViewById(R.id.loadingMessage)





            val initialCancelButton = findViewById<Button>(R.id.initialRenderCancelButton)
            val leftArrowInstruction = findViewById<TextView>(R.id.leftArrowInstruction)
            val stopButton = findViewById<Button>(R.id.stopButton)
            val endCaptureButton = findViewById<Button>(R.id.endCapture_button)
            val resumeCaptureButton = findViewById<Button>(R.id.resumeCapture_button)
            val mainScreenTitle = findViewById<TextView>(R.id.mainScreenTitle)
            val mainScreenSubTitle = findViewById<TextView>(R.id.mainScreenSubTitle)
            val imageCountText = findViewById<TextView>(R.id.imageCountText)
            val faceOutline = findViewById<ImageView>(R.id.faceOutline)
//            val plusButton = findViewById<Button>(R.id.plusButton)
//            val minusButton = findViewById<Button>(R.id.minusButton)

            val moveBackText = findViewById<TextView>(R.id.MoveBackText)
            val angleContainer = findViewById<ConstraintLayout>(R.id.angleContainer)
            faceRing=findViewById<ImageView>(R.id.faceRing)

            fun Int.toPx(): Int {
                val scale = resources.displayMetrics.density
                return (this * scale + 0.5f).toInt()
            }

            val params = faceRing.layoutParams
            params.width = cameraRingConfig.ringSize.toPx()
            params.height = cameraRingConfig.ringSize.toPx()
            faceRing.layoutParams = params

            faceOutline.scaleX = 1.1f
            faceOutline.scaleY = 1.1f
            minAngleText = findViewById<TextView>(R.id.minAngleText)
            maxAngleText = findViewById<TextView>(R.id.maxAngleText)
            referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "DEFAULT_REF"

            confirmButton.setOnClickListener{
                try {

                    Log.d("PlacementFlow","Confirm Clicked")
                    placeWhenTracking(

                        onSuccess = {},
                        onError = { error ->
                            runOnUiThread {
                                Toast.makeText(this, "Failed: $error. Try again.", Toast.LENGTH_LONG).show()
                                Log.e("Placement", "Error: $error")
                            }
                        }
                    )
                    cubeNode?.isVisible =true
                    faceOutline.visibility = View.GONE;
                    confirmButton.visibility = View.GONE;
                    mainScreenSubTitle.text = "Review the Planned Camera Poses"
                    initialCancelButton.visibility = View.GONE;
                    BackInStartCapture.visibility = View.VISIBLE
                    startButton.visibility = View.VISIBLE;
                    isFaceDetected = true
                    faceOverlayView.visibility = View.GONE
                    distanceLabel.visibility = View.GONE
                    moveBackText.visibility = View.VISIBLE



                }catch (e: Exception){
                    Log.d("PlacementFlow","Error in confirm button"+e.message.toString())
                }
            }
            initialCancelButton.setOnClickListener {
                try {
                    val intent = Intent(this, New_capture::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    Log.e("CancelButton", "Error starting activity", e)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }



//            var pivotLocalPos = Float3(0.261244386f, 0.09934329f, mapFloat(latestNosePosition.z*100+adjustedZ,-5f,-12f,-0.85133916f,-0.55133945f).toFloat())

//            fun shiftPivotBackBy(str: String) {
//                if (!::pivotNode.isInitialized) {
//                    Log.w("PivotShift", "pivotNode not initialized yet; ignoring shift.")
//                    return
//                }
//                // Subtract from z (e.g., z = -0.5713 -> -0.6213 if amount = 0.05)
//
//                if(str=="Plus"){
//                    pivotLocalPos = Float3(pivotLocalPos.x, pivotLocalPos.y, pivotLocalPos.z + 0.02f)
//                }else{
//                    pivotLocalPos = Float3(pivotLocalPos.x, pivotLocalPos.y, pivotLocalPos.z - 0.02f)
//                }
//
//                pivotNode.transform(position = pivotLocalPos, quaternion = pivotBaseQ)
//                Log.d("NewPositionIS","New Position is ${pivotLocalPos.z}")
//            }
//            plusButton.setOnClickListener {
//                shiftPivotBackBy("Plus")
//            }
//
//            minusButton.setOnClickListener {
//                shiftPivotBackBy("Minus")
//            }







            fun clearAnchorsAndNodes() {
                // Stop any pending countdown/capture
                try {
                    captureTimer?.cancel()
                    captureTimer = null
                    isCaptureInProgress = false
                } catch (_: Exception) {}

                // Remove ring/cube nodes from the pivot
                try {
                    nonCapturedModelList.forEach { node ->
                        runCatching { pivotNode.removeChildNode(node) }
                    }
                    nonCapturedModelList.clear()
                    capturedModelList.clear()
                    activeRing = null
                } catch (_: Exception) {}

                // Remove cube if present
                cubeNode?.let {
                    runCatching { pivotNode.removeChildNode(it) }
                }
                cubeNode = null

                // Detach and remove the pivot (if it was added)
                if (::pivotNode.isInitialized) {
                    runCatching { sceneView.removeChildNode(pivotNode) }
                }

                // Detach and remove the main anchor
                if (::anchorNode.isInitialized) {
                    runCatching { anchorNode.anchor?.detach() }
                    runCatching { sceneView.removeChildNode(anchorNode) }
                }

                // Safety: remove any stray AnchorNodes that may exist
                sceneView.childNodes
                    .filterIsInstance<AnchorNode>()
                    .forEach { an ->
                        runCatching { an.anchor?.detach() }
                        runCatching { sceneView.removeChildNode(an) }
                    }


                currentRingIndex = 0
                faceRing.visibility = View.GONE

            }








            BackInStartCapture.setOnClickListener {
                // Hide UI elements
                IsTrackingStarted=false

                BackInStartCapture.visibility = View.GONE
                mainScreenSubTitle.text = "Position the Subject in the Frame"
                startButton.visibility = View.GONE
                confirmButton.visibility = View.VISIBLE
                faceOverlayView.visibility = View.VISIBLE
                // Clear face detection state
                isFaceDetected = false

                if (!isFaceDetected) {

                    setConfirmEnabled(false)
                }
                initialCancelButton.visibility = View.VISIBLE
                faceOutline.visibility = View.VISIBLE
                currentRingIndex = 0 // Reset ring index
                moveBackText.visibility = View.GONE
                distanceLabel.visibility = View.VISIBLE
                // Reset any other relevant state
                updateDistanceLabel("Subject not in Frame")

                //remove all anchors and nodes
                clearAnchorsAndNodes()

            }








            startButton.setOnClickListener {

                // Remove the cube if it exists
                cubeNode?.let {
                    pivotNode.removeChildNode(it)
                    cubeNode = null

                }

                //activating the first ring 0

                nonCapturedModelList.forEachIndexed {index, node ->
                    if(node.name=="0"){

                        node.scale=Float3(
                            node.scale.x * faceRingConfig.ringScaleFactor,
                            node.scale.y * faceRingConfig.ringScaleFactor,
                            node.scale.z
                        )
                        activeRing=node;
                    } }

                faceRing.visibility=View.VISIBLE
                distanceLabel.visibility = View.VISIBLE
                moveBackText.visibility = View.GONE
                angleContainer.visibility = View.VISIBLE;
                mainScreenTitle.text = "Capture"
                mainScreenSubTitle.visibility = View.GONE
                imageCountText.visibility = View.VISIBLE
                imageCountText.setText("${capturedModelList.size}/${nonCapturedModelList.size}")
                startButton.visibility = View.GONE
                BackInStartCapture.visibility = View.GONE
                stopButton.visibility = View.VISIBLE
                leftArrowInstruction.visibility = View.VISIBLE
                startBlinkingAnimation(leftArrowInstruction)
                leftArrowInstruction.postDelayed({
                    leftArrowInstruction.visibility = View.GONE
                    //will start Tracking
                    IsTrackingStarted=true
                    initSensorListener()}, 3000)
            }




            stopButton.setOnClickListener {
                stopButton.visibility = View.GONE
                if (IsCaptureStarted) {
                    IsCaptureStarted = false
                    IsTrackingStarted=false
                    updateDistanceLabel("Paused");
                }
                endCaptureButton.visibility = View.VISIBLE
                resumeCaptureButton.visibility = View.VISIBLE

            }

            resumeCaptureButton.setOnClickListener {
                if (!IsCaptureStarted) {
                    IsCaptureStarted = true
                    IsTrackingStarted=true
                }
                resumeCaptureButton.visibility = View.GONE
                endCaptureButton.visibility = View.GONE
                stopButton.visibility = View.VISIBLE;


            }

            endCaptureButton.setOnClickListener {
                val dialog = Dialog(this)

                val view = layoutInflater.inflate(R.layout.modal_capture_end, null)
                val noButton = view.findViewById<Button>(R.id.endCaptureNoBtn)

                val imageCount = capturedModelList.size // adding image count to modal
                val text = getString(R.string.endcaptureText, imageCount)
                val endText = view.findViewById<TextView>(R.id.endCaptureTextLabel)

                endText.text = text

                val yesButton = view.findViewById<Button>(R.id.endCaptureYesBtn)

                noButton.setOnClickListener {
                    dialog.dismiss()
                    if (!IsCaptureStarted) {
                        IsCaptureStarted = true
                    }
                    resumeCaptureButton.visibility = View.GONE
                    endCaptureButton.visibility = View.GONE
                    stopButton.visibility = View.VISIBLE;


                }

                yesButton.setOnClickListener {
                    if (capturedModelList.size== 0) {
                        val intent = Intent(this, New_capture::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val intent = Intent(this, completeCapture::class.java)
                        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
                        intent.putExtra("IMAGE_COUNT", capturedModelList.size.toString())
                        intent.putExtra("TOTAL_IMAGE_COUNT",nonCapturedModelList.size.toString())
                        startActivity(intent)

                    }

                }

                dialog.setContentView(view)

                dialog.getWindow()?.setBackgroundDrawableResource(android.R.color.transparent)
                val metrics = resources.displayMetrics
                val screenWidth = metrics.widthPixels
                val marginInPx = (20 * metrics.density).toInt()
                val dialogWidth = screenWidth - (marginInPx * 2)
                dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                dialog.show()
            }






            // Check for camera permissions
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            } else {

            }



        } catch (e: Exception) {

            e.printStackTrace()
            Log.d("onCreate", "onCreate Error  " + e.message.toString())
        }


    }

    private fun setupMediaPipeLandmarker() {
        try {
            val base = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setNumFaces(1)
                .setRunningMode(RunningMode.IMAGE) // we pass a Bitmap each time
                .build()

            mpFaceLandmarker = FaceLandmarker.createFromOptions(this, options)
            mpReady = true
            Log.d("MP", "Face Landmarker ready")
        } catch (e: Exception) {
            mpReady = false
            Log.e("MP", "Failed to init Face Landmarker: ${e.message}")
            Toast.makeText(this, "Face Landmarker init failed", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::faceThread.isInitialized) {
            faceThread.quitSafely()
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun applyOrbitYaw(deg: Float) {
        Log.d("NewPositionIS","Angle was $deg")
        val yawQ = Quaternion.fromAxisAngle(Direction(0f, 1f, 0f), deg)
        val finalQ = pivotBaseQ * yawQ
        pivotNode?.transform(quaternion = finalQ)
    }

    fun mapFloat(x: Double, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Double {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }


    fun placeWhenTracking(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        Log.d("PlacementFlow","Done 1")
        val frame = sceneView.session?.frame ?: return onError("Camera frame not available")
        Log.d("PlacementFlow","Done 2")
        if (frame.camera.trackingState != TrackingState.TRACKING) {
            sceneView.postDelayed({ placeWhenTracking(onSuccess, onError) }, 200)
            return
        }

        Log.d("PlacementFlow","Done 3")

        try {
            Log.d("PlacementFlow","Done 4")
            val session = sceneView.session ?: return onError("No AR session")
            val cameraPose = frame.camera.pose

            // Anchor at camera position WITH yaw baked in
            val anchorPose = Pose(
                floatArrayOf(cameraPose.tx(), cameraPose.ty(), cameraPose.tz()),
                floatArrayOf(cameraPose.qx(), cameraPose.qy(), cameraPose.qz(),cameraPose.qw()),
            )
            val anchor = session.createAnchor(anchorPose)
            anchorNode = AnchorNode(sceneView.engine, anchor).apply { isEditable = true }
            sceneView.addChildNode(anchorNode)

            //computeHere
            val xPoint = 0.261244386f             //up down
            val yPoint = 0.09934329f-0.02f
//            val zPoint = -0.57133945f

            val zPoint=mapFloat(latestNosePosition.z*100+adjustedZ,-5f,-12f,-0.85133916f,-0.55133945f)
            Log.d("NewPositionIS"," Adjusted BY ${latestNosePosition.z*100+adjustedZ}")








            // 🟡 Position offset for this circle
            val offsetVector = Vector3(
                xPoint.toFloat(),
                yPoint.toFloat(),
                zPoint.toFloat()
            )
            val offsetFloat3 = Float3(offsetVector.x, offsetVector.y, offsetVector.z)





            val orbitNode = Node(sceneView.engine).apply {parent = anchorNode }
            pivotNode = orbitNode
            val correctionQ = createQuaternionFromAxisAngle(Vector3(0f, 0f, 1f), 90f)
            pivotBaseQ = correctionQ
            pivotNode.transform(position = offsetFloat3, quaternion = correctionQ)


            pivotNode.transform(
                position = offsetFloat3,
                quaternion = correctionQ
            )



            if(yawLRDegG<=10f && yawLRDegG>=-10f){
                applyOrbitYaw(0f)
            }else{
                var shiftBy=0.0f;

                if(yawLRDegG<0){
                    shiftBy=yawLRDegG-10f
                }else{
                    shiftBy=yawLRDegG+10f
                }
                applyOrbitYaw(shiftBy)
            }











            lifecycleScope.launch {
                try {

                    placeCube()
                    placeCirclesAroundFace()
                    onSuccess()
                } catch (e: Exception) {
                    Log.d("ConfirmButtonLogs", "Error lifecycleScope.launch: ${e.message}")
                    onError(e.message ?: "Unknown error placing models")
                }
            }
        } catch (e: Exception) {
            Log.d("ConfirmButtonLogs", "Error in function: ${e.message}")
            onError(e.message ?: "Failed to create anchor")
        }
    }








    // Function to place the cube model on head
    private fun placeCube() {
        try {
            // Keep any orientation you want for the cube
            val rotationX = createQuaternionFromAxisAngle(Vector3(1f, 0f, 0f), 0f)
            val rotationY = createQuaternionFromAxisAngle(Vector3(0f, 1f, 0f), 0f)
            val rotationZ = createQuaternionFromAxisAngle(Vector3(0f, 0f, 1f), 0f)
            val combinedRotation3 = (rotationZ * rotationY * rotationX).toEulerAngles()

            lifecycleScope.launch {
                try {
                    sceneView.modelLoader.loadModelInstance(faceCubeUri)?.let { modelInstance ->
                        modelInstance.materialInstances.forEach { _ ->
                            cubeNode = ModelNode(
                                modelInstance = modelInstance,
                                scaleToUnits = cubeConfig.cubeSize,
                                autoAnimate = true,
                                centerOrigin = null
                            ).apply {
                                isPositionEditable = false
                                isVisible = true  //hides the modal
                                // ⬇️ No offset. Cube sits at pivot origin.
                                transform(position = Float3(0f, 0f, 0f), rotation = combinedRotation3)
                            }

                            cubeNode?.let { pivotNode.addChildNode(it) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CubePlacement", "Error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("placeCube", "Error: ${e.message}", e)
            showToast("Something went wrong. Try again!")
        }
    }


    // The function responsible for placing the circles around the face .It loops arrowList which contains all the circles configurations and places them accordingly
    private fun placeCirclesAroundFace() {

        try {
            arrowList.forEachIndexed{ index, bulletObj ->
                // Position offset for this circle

                //computeHere
                val offsetVector = Vector3(
                    bulletObj.xPoint.toFloat()-0.08f,
                    bulletObj.yPoint.toFloat()+0.25f,
                    bulletObj.zPoint.toFloat() +0.16f
                )
                val offsetFloat3 = Float3(offsetVector.x, offsetVector.y, offsetVector.z)

                var verticalAngle=-bulletObj.verticalAngle.toFloat();
                var horizontalAngle=-bulletObj.horizontalAngle.toFloat();



                // Rotation logic — replace with actual angles when needed
                val rotationX = createQuaternionFromAxisAngle(Vector3(1f, 0f, 0f), verticalAngle);
                val rotationY = createQuaternionFromAxisAngle(Vector3(0f, 1f, 0f),horizontalAngle)
                val rotationZ = createQuaternionFromAxisAngle(Vector3(0f, 0f, 1f), 0f)
                val combinedRotation = rotationZ * rotationY * rotationX
                val combinedRotation3=combinedRotation.toEulerAngles()


                // Load and place model asynchronously
                lifecycleScope.launch {
                    try {
                        val modelNode = makeCircleModel()
                        modelNode?.let {
                            it.transform(position = offsetFloat3, rotation = combinedRotation3)


                            pivotNode?.addChildNode(it)
                            //adding model node to a mutable list
                            it.isEditable=false;
                            it.isPositionEditable=false
                            it.name="${bulletObj.seqID}"
                            nonCapturedModelList.add(modelNode) }


                    } catch (e: Exception) {
                        Log.e("ArrowLoad", "Error loading arrow: ${e.message}", e)
                    }
                }
            }



        } catch (e: Exception) {
            Log.e("placeCirclesAroundFace", "Error: ${e.message}", e)
            showToast("Something Went wrong. Try again !")
        }
    }


    private fun initializeARScene() {
        Log.d("initializeARScene", "initializeARScene called")
        sceneView = findViewById<ARSceneView>(R.id.arSceneView).apply {
            lifecycle = this@MainActivity.lifecycle

            configureSession { session, config ->
                try {
                    when (ArCoreApk.getInstance().checkAvailability(this@MainActivity)) {
                        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                            showToast("ARCore not supported !")
                            finish()
                        }
                        else -> {}
                    }

                    config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                        Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.focusMode = Config.FocusMode.AUTO
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

                } catch (e: UnavailableException) {
                    showToast("ARCore init failed: ${e.message}")
                    Log.e("ARCore", "Init failed", e)
                    finish()
                }
            }

            planeRenderer.isVisible = false

            onSessionCreated = { session ->
                lifecycleScope.launch {
                    // Start face detection immediately
                    startFaceDetection()
                }
            }


            onSessionFailed = { exception ->
                Log.e("ARCore", "Session failed", exception)
                showToast("Session error: ${exception.message}")
            }
        }
    }




    //Function to track camera movement speed to warn user to move slowly
    private fun checkMovementSpeed(currentPosition: Vector3) {
        val currentTime = System.currentTimeMillis()

        previousPosition?.let { prevPos ->
            if (previousTime > 0) {
                val distance = calculateDistance(prevPos, currentPosition)
                val timeDelta = (currentTime - previousTime) / 1000f // seconds
                if (timeDelta > 0) {
                    val speed = distance / timeDelta

                    if (speed > maxAllowedSpeed && currentTime - lastToastTime > toastCooldown) {

                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Please move slowly !",
                                Toast.LENGTH_SHORT
                            ).show()
                            lastToastTime = currentTime
                        }
                    }
                }
            }
        }

        // Update previous values for next frame
        previousPosition = currentPosition
        previousTime = currentTime
    }





    fun Position.length(): Float {
        return sqrt(x * x + y * y + z * z)
    }




    // Function to create quaternions from axis-angle representation
    private fun createQuaternionFromAxisAngle(axis: Vector3, angleDegrees: Float): Quaternion {
        val angleRad = Math.toRadians(angleDegrees.toDouble()).toFloat()
        val halfAngle = angleRad / 2f
        val sinHalf = kotlin.math.sin(halfAngle)
        val cosHalf = kotlin.math.cos(halfAngle)
        val normAxis = axis.normalized()
        return Quaternion(
            normAxis.x * sinHalf,
            normAxis.y * sinHalf,
            normAxis.z * sinHalf,
            cosHalf
        )
    }







    //Function which captures the image and activates the next ring to be captured.
    private fun captureAndNextRing(){
        val imageCountText = findViewById<TextView>(R.id.imageCountText)


        showCaptureBlinkEffect()
        captureCameraFeedPhoto(sceneView)



        //Again reducing the size of ring which has been captured .
        nonCapturedModelList.forEachIndexed {index, node ->
            Log.d("reducingSize","index is$index and seq is ${node.name}")
            if(node.name==currentRingIndex.toString()){

                node.scaleToUnitCube(faceRingConfig.initialRingSize)

                Log.d("reducingSize"," satisfied with index is$index and seq is $node.name")
                node.model.instance.materialInstances.forEach { materialInstance ->
                    materialInstance.setColor(
                        name = "baseColorFactor",
                        color = "#3FFF1B".toColorInt(), // HEX to Int
                        type = Colors.RgbaType.SRGB
                    )

                }

            }


        }



        capturedModelList.add(nonCapturedModelList[currentRingIndex])// Add the captured ring node to the captured list
        imageCountText.setText("${capturedModelList.size}/${nonCapturedModelList.size}")//update image capture count in UI
        currentRingIndex++;
        // Show next ring

        if (currentRingIndex < nonCapturedModelList.size) {  //20



            nonCapturedModelList.forEachIndexed {index, node ->
                if(node.name==currentRingIndex.toString()){

                    node.scale=Float3(
                        node.scale.x * faceRingConfig.ringScaleFactor, // Scale X
                        node.scale.y *  faceRingConfig.ringScaleFactor, // Scale Y
                        node.scale.z         // Keep Z as is
                    ) }




            }
        }
        //showing dialog on capture complete
        if (capturedModelList.size == nonCapturedModelList.size) {
            Log.d("showCompletionDialog","showCompletionDialog will show dialog box")
            showCompletionDialog()
        }
    }

    //function to show capture blink effect while image is captured
    private fun startBlinkingAnimation(textView: TextView) {
        val animator =
            ValueAnimator.ofFloat(1f, 0f, 1f) // From fully visible to invisible to visible
        animator.duration = 1000 // 500ms per blink
        animator.repeatCount = ValueAnimator.INFINITE // Keep repeating
        animator.addUpdateListener { animation ->
            val alphaValue = animation.animatedValue as Float
            textView.alpha = alphaValue // Set alpha value for blinking effect
            textView.elevation = 10 * (1 - alphaValue) // Simulate shadow elevation
        }
        animator.start()
    }

    private fun readFxFyCxCy(camIntr: com.google.ar.core.CameraIntrinsics): Intrinsics {
        // Try struct-style first (.x/.y). If that fails at compile time, comment block A and use block B.
        // ---------- Block A: struct-style ----------

        val fxA = camIntr.focalLength[0].toDouble()

        val fyA = camIntr.focalLength[1].toDouble()
        val cxA = camIntr.principalPoint[0].toDouble()
        val cyA = camIntr.principalPoint[1].toDouble()
        return Intrinsics(fxA, fyA, cxA, cyA)

        // ---------- Block B: array-style (uncomment if your SDK returns float[]) ----------
        // val fl = camIntr.focalLength            // float[2]
        // val pp = camIntr.principalPoint         // float[2]
        // return Intrinsics(fl[0].toDouble(), fl[1].toDouble(), pp[0].toDouble(), pp[1].toDouble())
    }

    private fun scaleIntrinsicsToImage(
        intr: Intrinsics,
        intrDims: IntArray,            // from camera.textureIntrinsics.imageDimensions
        imageW: Int, imageH: Int       // the EXACT width/height used to feed FaceMesh and build img_pts
    ): Intrinsics {
        val srcW = intrDims[0].toDouble()
        val srcH = intrDims[1].toDouble()
        val sx = imageW / srcW
        val sy = imageH / srcH
        Log.i("OPENCV","LINE6")

        return Intrinsics(
            fx = intr.fx * sx,
            fy = intr.fy * sy,
            cx = intr.cx * sx,
            cy = intr.cy * sy
        )
    }






    //Function which only shows the front facing bullets and hides the rest
    private fun checkVisibilityOfBullets() {
        val frame = sceneView.frame ?: return
        val cameraPose = frame.camera.pose
        val cameraPosition = Vector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
        val cameraForward = Vector3(-cameraPose.zAxis[0], -cameraPose.zAxis[1], -cameraPose.zAxis[2]).normalized()

        // ✅ Use closest bullet as reference
        val referenceBullet = nonCapturedModelList.minByOrNull { bullet ->
            Vector3.subtract(bullet.worldPosition.toVector3(), cameraPosition).length()
        } ?: return

        val refVector = Vector3.subtract(referenceBullet.worldPosition.toVector3(), cameraPosition)
        val referenceDepth = Vector3.dot(refVector, cameraForward)

        val dynamicMin = 0.6f * referenceDepth
        val dynamicMax = 1.5f * referenceDepth

        for (bulletNode in nonCapturedModelList) {
            val bulletVector = Vector3.subtract(bulletNode.worldPosition.toVector3(), cameraPosition)
            val depth = Vector3.dot(bulletVector, cameraForward)
            val directionToBullet = bulletVector.normalized()
            val dotFacing = Vector3.dot(cameraForward, directionToBullet)

            if (depth in dynamicMin..dynamicMax && dotFacing > 0.5f) {
                bulletNode.isVisible = true // show the rings
            } else {
                bulletNode.isVisible = false
            }
        }
    }





    var loggedInitialCamera = false
    private fun startFaceDetection() {
        updateDistanceLabel("Subject not in Frame")

        // Set up frame processing
        sceneView.onSessionUpdated = onSessionUpdated@{ session, frame ->

            if (!loggedInitialCamera) {
                initialCameraPose = frame.camera.pose
                loggedInitialCamera = true
            }

            if(IsTrackingStarted){
                startTrackingRings(frame);
            }
//            getFaceDistanceMeters()?.let { (distanceM, depthM) ->
//                // distanceM = straight-line distance camera→nose (meters)
//                // depthM    = Z forward distance (meters)
//                Log.i("FaceDistance", "distance=${"%.2f".format(distanceM)} m, depth=${"%.2f".format(depthM)} m")
//            }

            if (lastMpLandmarks.isNotEmpty()) {
                logFaceToCameraAngles()
            }


            val cameraPosition = frame.camera.pose
            latestCamPosition=Point3D(cameraPosition.tx().toDouble(),cameraPosition.ty().toDouble(), cameraPosition.tz().toDouble())

            checkVisibilityOfBullets()

//            val now = System.currentTimeMillis()
//            val frameIntervalMs = 7L   // ~30 FPS
//
//            if ((now - lastFaceDetectTime) >= frameIntervalMs && faceOverlayView.visibility == View.VISIBLE) {
//                processFrameForFaceDetection(frame)
//                lastFaceDetectTime = now
//            }

            // 30 FPS gate + overlap guard
            val ts = frame.timestamp // in ns
            val due = ts - lastDispatchTimestampNs >= MIN_INTERVAL_NS
            if (!due || isProcessingFrame) return@onSessionUpdated

            try {
                // Acquire **this frame’s** camera image on the render thread
                val image = frame.acquireCameraImage()

                // Mark dispatch time now, so we keep a stable cadence
                lastDispatchTimestampNs = ts
                isProcessingFrame = true

                // Hand off heavy work to background thread
                faceHandler.post {
                    try {
                        val bmp = convertImageToBitmap(image) // your helper
                        // Inside this you can rotate, run MediaPipe, update UI via runOnUiThread
                        processBitmapForFaceDetection(bmp)
                    } catch (t: Throwable) {
                        android.util.Log.e("FaceDetection", "Worker error: ${t.message}", t)
                    } finally {
                        try { image.close() } catch (_: Exception) {}
                        isProcessingFrame = false
                    }
                }
            } catch (_: Exception) {
                // No camera image available this frame; skip gracefully
            }


        }
    }





    // ====== OpenCV helpers ======
    private fun mat33(vararg m: Double): Mat {
        require(m.size == 9) { "mat33 needs 9 values" }
        return Mat(3, 3, org.opencv.core.CvType.CV_64F).apply { put(0, 0, *m) }
    }

    private fun matOfDouble(vararg v: Double): Mat =
        Mat(v.size, 1, org.opencv.core.CvType.CV_64F).apply {
            put(0, 0, *v)   // <-- spread
        }



    private fun rvecToQuaternionFromR(R: Mat): dev.romainguy.kotlin.math.Quaternion {
        val rvec = Mat()
        org.opencv.calib3d.Calib3d.Rodrigues(R, rvec)
        val rx = rvec.get(0,0)[0]; val ry = rvec.get(1,0)[0]; val rz = rvec.get(2,0)[0]
        val angle = kotlin.math.sqrt(rx*rx + ry*ry + rz*rz)
        if (angle < 1e-9) return dev.romainguy.kotlin.math.Quaternion(0f,0f,0f,1f)
        val ax = (rx/angle).toFloat(); val ay = (ry/angle).toFloat(); val az = (rz/angle).toFloat()
        val half = (angle*0.5).toFloat()
        val s = kotlin.math.sin(half); val c = kotlin.math.cos(half)
        return dev.romainguy.kotlin.math.Quaternion(ax*s, ay*s, az*s, c)
    }

    private fun cvToArPos(x: Double, y: Double, z: Double) =
        dev.romainguy.kotlin.math.Float3(x.toFloat(), (-y).toFloat(), (-z).toFloat())

    private fun cvToArRot(Rcv: Mat): Mat {
        val S = mat33(
            1.0, 0.0, 0.0,
            0.0,-1.0, 0.0,
            0.0, 0.0,-1.0
        )
        val tmp = Mat()
        val Rar = Mat()
        org.opencv.core.Core.gemm(S, Rcv, 1.0, Mat(), 0.0, tmp)     // tmp = S * Rcv
        org.opencv.core.Core.gemm(tmp, S, 1.0, Mat(), 0.0, Rar)     // Rar = tmp * S
        return Rar
    }



    private fun ema(pPrev: dev.romainguy.kotlin.math.Float3?, pNew: dev.romainguy.kotlin.math.Float3, a: Float)
            : dev.romainguy.kotlin.math.Float3 =
        if (pPrev == null) pNew else dev.romainguy.kotlin.math.Float3(
            pPrev.x + a*(pNew.x - pPrev.x),
            pPrev.y + a*(pNew.y - pPrev.y),
            pPrev.z + a*(pNew.z - pPrev.z)
        )

    private fun slerp(q1: dev.romainguy.kotlin.math.Quaternion,
                      q2: dev.romainguy.kotlin.math.Quaternion,
                      t: Float): dev.romainguy.kotlin.math.Quaternion {
        var qb = q2
        var cos = q1.x*qb.x + q1.y*qb.y + q1.z*qb.z + q1.w*qb.w
        if (cos < 0f) { qb = dev.romainguy.kotlin.math.Quaternion(-qb.x,-qb.y,-qb.z,-qb.w); cos = -cos }
        if (cos > 0.9995f) {
            val x = q1.x + t*(qb.x - q1.x); val y = q1.y + t*(qb.y - q1.y); val z = q1.z + t*(qb.z - q1.z); val w = q1.w + t*(qb.w - q1.w)
            val inv = 1f/kotlin.math.sqrt(x*x + y*y + z*z + w*w)
            return dev.romainguy.kotlin.math.Quaternion(x*inv,y*inv,z*inv,w*inv)
        }
        val theta = kotlin.math.acos(cos)
        val sinT = kotlin.math.sin(theta)
        val w1 = kotlin.math.sin((1f - t)*theta)/sinT
        val w2 = kotlin.math.sin(t*theta)/sinT
        return dev.romainguy.kotlin.math.Quaternion(
            q1.x*w1 + qb.x*w2, q1.y*w1 + qb.y*w2, q1.z*w1 + qb.z*w2, q1.w*w1 + qb.w*w2
        )
    }

    // Rodrigues rvec -> Quaternion (unchanged from before)
    private fun rvecToQuaternion(rvec: Mat): dev.romainguy.kotlin.math.Quaternion {
        val rx = rvec.get(0,0)[0]; val ry = rvec.get(1,0)[0]; val rz = rvec.get(2,0)[0]
        val angle = kotlin.math.sqrt(rx*rx + ry*ry + rz*rz)
        if (angle < 1e-9) return dev.romainguy.kotlin.math.Quaternion(0f,0f,0f,1f)
        val ax = (rx/angle).toFloat(); val ay = (ry/angle).toFloat(); val az = (rz/angle).toFloat()
        val half = (angle*0.5).toFloat()
        val s = kotlin.math.sin(half); val c = kotlin.math.cos(half)
        return dev.romainguy.kotlin.math.Quaternion(ax*s, ay*s, az*s, c)
    }

    private fun normalize3(x: Double, y: Double, z: Double): Triple<Float, Float, Float> {
        val n = kotlin.math.sqrt(x*x + y*y + z*z).coerceAtLeast(1e-9)
        return Triple((x/n).toFloat(), (y/n).toFloat(), (z/n).toFloat())
    }

    /** Minimal canonical 3D model (meters). +Z out of face. Keep origin at nose tip. */
    private data class LM(val idx: Int, val name: String)
    private val LM_SET = listOf(
        LM(1,   "nose_tip"),
        LM(33,  "left_eye_outer"),
        LM(263, "right_eye_outer"),
        LM(61,  "mouth_left"),
        LM(291, "mouth_right"),
        LM(199, "chin"),
        LM(9,   "glabella")
    )
    private val MODEL_3D: Map<String, DoubleArray> = mapOf(
        "nose_tip"        to doubleArrayOf( 0.000,  0.000,  0.000),
        "left_eye_outer"  to doubleArrayOf(-0.0325, 0.0220, 0.0280),
        "right_eye_outer" to doubleArrayOf( 0.0325, 0.0220, 0.0280),
        "mouth_left"      to doubleArrayOf(-0.0280,-0.0200, 0.0200),
        "mouth_right"     to doubleArrayOf( 0.0280,-0.0200, 0.0200),
        "chin"            to doubleArrayOf( 0.000,-0.0550,-0.0100),
        "glabella"        to doubleArrayOf( 0.000, 0.0350, 0.0350)
    )
    // Build camera matrix from ARCore
    private fun cameraMatrixFromAR(cam: com.google.ar.core.Camera): Mat {
        val fx = cam.textureIntrinsics.focalLength[0].toDouble()
        val fy = cam.textureIntrinsics.focalLength[1].toDouble()
        val cx = cam.textureIntrinsics.principalPoint[0].toDouble()
        val cy = cam.textureIntrinsics.principalPoint[1].toDouble()
        return mat33(
            fx, 0.0, cx,
            0.0, fy, cy,
            0.0, 0.0, 1.0
        )
    }

    /** Convert MediaPipe normalized 2D landmarks -> pixel points using ARCore texture size */
    private fun mp2dToPixels(
        lms: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        cam: com.google.ar.core.Camera
    ): org.opencv.core.MatOfPoint2f {
        val w = cam.textureIntrinsics.imageDimensions[0].toDouble()
        val h = cam.textureIntrinsics.imageDimensions[1].toDouble()
        val pts = java.util.ArrayList<org.opencv.core.Point>(LM_SET.size)
        for (lm in LM_SET) {
            val p = lms[lm.idx]
            pts.add(org.opencv.core.Point(p.x() * w, p.y() * h))
        }
        return org.opencv.core.MatOfPoint2f(*pts.toTypedArray())
    }

    private fun clamp01(n: Double) = when {
        n < -1.0 -> -1.0
        n >  1.0 ->  1.0
        else -> n
    }

    /** 3D model points for our chosen landmarks */
    private fun model3dMat(): org.opencv.core.MatOfPoint3f {
        val arr = LM_SET.map { lm -> val v = MODEL_3D[lm.name]!!; org.opencv.core.Point3(v[0], v[1], v[2]) }
        return org.opencv.core.MatOfPoint3f(*arr.toTypedArray())
    }

    private var lastCubeEdgeM = -1f
    // Helper: degrees → radians
    private fun rad(deg: Float) = Math.toRadians(deg.toDouble()).toFloat()

    private var cubeLoadInFlight = false


    private fun radToDeg(x: Float) = Math.toDegrees(x.toDouble()).toFloat()
    private fun eulerRadToDeg(e: dev.romainguy.kotlin.math.Float3) =
        dev.romainguy.kotlin.math.Float3(radToDeg(e.x), radToDeg(e.y), radToDeg(e.z))






    private fun logFaceToCameraAngles() {
        val frame = sceneView.session?.frame ?: return
        val cam = frame.camera
        if (lastMpLandmarks.isEmpty()) return

        try {
            // Build PnP inputs (reuse your existing helpers)
            val objPts = model3dMat()
            val imgPts = mp2dToPixels(lastMpLandmarks, cam)
            val K      = cameraMatrixFromAR(cam)
            val dist   = MatOfDouble(Mat.zeros(1, 5, org.opencv.core.CvType.CV_64F))

            // Solve head->camera
            val rvec = Mat()
            val tvec = Mat()
            org.opencv.calib3d.Calib3d.solvePnP(
                objPts, imgPts, K, dist, rvec, tvec, false,
                org.opencv.calib3d.Calib3d.SOLVEPNP_ITERATIVE
            )

            // Face forward in camera coords = third column of R
            val R = Mat()
            org.opencv.calib3d.Calib3d.Rodrigues(rvec, R)
            val fx = R.get(0, 2)[0]
            val fy = R.get(1, 2)[0]
            val fz = R.get(2, 2)[0]
            val n  = kotlin.math.sqrt(fx*fx + fy*fy + fz*fz).coerceAtLeast(1e-9)
            val dx = fx / n
            val dy = fy / n
            val dz = fz / n

            // Camera optical axis in OpenCV coords is (0,0,1)
            val dot = clamp01(dz)           // since dot = (0,0,1) · (dx,dy,dz) = dz
            val angleRad  = kotlin.math.acos(dot)
            val angleDeg  = Math.toDegrees(angleRad)             // 0° = facing camera; 90° = pure side

            // Signed left/right (yaw-ish): +right, -left
            val yawLRDeg  = Math.toDegrees(kotlin.math.atan2(dx, dz))

            // Signed up/down (pitch-ish): +up, -down
            val pitchUDeg = Math.toDegrees(kotlin.math.atan2(-dy, kotlin.math.sqrt(dx*dx + dz*dz)))


            angleLogFrame = (angleLogFrame + 1) % 3
            if (angleLogFrame == 0) {
                val yawLRDegG_1 = if(yawLRDeg<0){
                    -(180+yawLRDeg.toFloat())
                }else{
                    180-yawLRDeg.toFloat()
                }
                val cleanDeg = angleFilter.update(yawLRDegG_1)
                yawLRDegG = cleanDeg
                // Show on screen
                updateAngleHud(yawLRDegG)
                Log.e("FaceToCamera", "Angle : ${yawLRDegG}")
            }


        } catch (e: Exception) {
            Log.e("FaceToCamera", "Angle compute failed: ${e.message}")
        }
    }


    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }


    private fun showDebug (Text: String){
        runOnUiThread {
            val debugText=findViewById<TextView>(R.id.debugText)
            debugText.visibility=View.VISIBLE
            debugText.text=Text
        }

    }





    // Function detects the face and updates overlay & UI using MediaPipe Face Landmarker
    private fun processBitmapForFaceDetection(bitmap: Bitmap) {
        try {
            if (!mpReady) {

                setConfirmEnabled(false)
                return
            }
            // --- ensure detector & overlay see the same orientation ---
            val rotation = getRotationCompensation("0", isFrontFacing = false) // back cam
            val bmp = rotateBitmap(bitmap, rotation)

            val mpImage: MPImage = BitmapImageBuilder(bmp).build()
            val rotationDegrees = getRotationCompensation("0", isFrontFacing = false) // back camera
            val ipOpts = ImageProcessingOptions.builder()
                .setRotationDegrees(rotationDegrees)
                .build()
            val result = mpFaceLandmarker.detect(mpImage,ipOpts)

            if (result.faceLandmarks().isEmpty()) {
                isAnyFace = false
                isFaceDetected = false
                lastMpLandmarks = emptyList()
                faceOverlayView.updatePointsPx(emptyList(), 0, 0, null) // uses the new overload
                updateDistanceLabel("Subject not in Frame")
                setConfirmEnabled(false)
                hasResetForCurrentFace = false
                return
            }




            val landmarks = result.faceLandmarks()[0]
            lastMpLandmarks = landmarks

            val pixelPoints1 = landmarks.map { android.graphics.PointF(it.x() * bmp.width, it.y() * bmp.height) }


// call the overload with triangles
            faceOverlayView.updatePointsPx(
                pointsPx = pixelPoints1,
                trianglesIdx = FaceMeshTriangulation.triangles,
                w = bmp.width,
                h = bmp.height,
                box = null
            )

            val nose = landmarks[1]


            latestNosePosition = Point3D((nose.x() * bmp.width).toDouble(), (nose.y() * bmp.height).toDouble(), nose.z()
                .toDouble())





            val noseZ=latestNosePosition.z*100
            val noseDistance= String.format(Locale.US, "Face Distance %.1f",noseZ)

            showDebug(noseDistance)
            when {
                //approximateValues
                noseZ < cameraConfigDetection.minDistance -> {
                    Log.d(
                        "isFaceDetected",
                        "updateDistanceLabel Move Back" + isFaceDetected
                    )
                    updateDistanceLabel("Move Away")
                    setConfirmEnabled(false)
                }

                noseZ >cameraConfigDetection.maxDistance -> {
                    Log.d(
                        "isFaceDetected",
                        "updateDistanceLabel MOVE Close" + isFaceDetected
                    )
                    updateDistanceLabel("Move Closer")
                    setConfirmEnabled(false)
                }

                else -> {
                    Log.d("isFaceDetected", "updateDistanceLabel DETECTED" + noseZ)
                    updateDistanceLabel("Subject Detected")
                    isAnyFace = true
                    isFaceDetected = true
                    setConfirmEnabled(true)

                }
            }







        } catch (e: Exception) {
            Log.e("FaceLandmarker", "Error: ${e.message}")

            setConfirmEnabled(false)
        }
    }





































    //Function to start tracking the circles around face which helps the  user to capture image according to his desired position
    private fun startTrackingRings(frame: Frame) {
        if (nonCapturedModelList.isEmpty()) return

        val cameraPose = frame.camera.pose
        val cameraPosition = Vector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
        checkMovementSpeed(cameraPosition)
        checkVisibilityOfBullets()
        if (currentRingIndex < nonCapturedModelList.size) {
            nonCapturedModelList.forEachIndexed { index, node ->
                if (node.name == currentRingIndex.toString()) {
                    activeRing = node
                }
            }

            val activeRingPosition = (activeRing!!).worldPosition.toVector3()
            val distance = calculateDistance(cameraPosition, activeRingPosition)
            val ringAngle = arrowList[currentRingIndex].verticalAngle

            when {
                distance >= cameraConfigCapture.minDistance &&
                        distance <= cameraConfigCapture.maxDistance -> {

                    if (angleString>=ringAngle-1 && angleString<=ringAngle+1)
                    {
                        updateDistanceLabel("Hold Still")


                        if (IsCaptureStarted && !isCaptureInProgress) {
                            isCaptureInProgress = true

                            // Start a countdown timer before capture
                            captureTimer = object : CountDownTimer(delayCaptureBy.toLong(), 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    val secondsLeft = millisUntilFinished / 1000

                                }

                                override fun onFinish() {
                                    Log.d("CaptureTimer", "Timer finished -> Capturing now")
                                    captureAndNextRing()
                                    isCaptureInProgress = false
                                    captureTimer = null
                                }
                            }.start()
                        }

                    } else {
                        cancelScheduledCapture()
                        updateDistanceLabel("Adjust angle")
                        faceRing.setBackgroundResource(R.drawable.circle_ring)
                    }

                }

                distance > cameraConfigCapture.maxDistance -> {
                    cancelScheduledCapture()
                    updateDistanceLabel("Move Closer")
                    faceRing.setBackgroundResource(R.drawable.circle_ring)
                }

                distance < cameraConfigCapture.minDistance -> {
                    cancelScheduledCapture()
                    updateDistanceLabel("Move Away")
                    faceRing.setBackgroundResource(R.drawable.circle_ring)
                }
            }


            minAngleText.text = "Ring  :${currentRingIndex}"
            maxAngleText.text = "Angle  :${ringAngle}"
        }

    }

    private fun cancelScheduledCapture() {
        captureTimer?.cancel()
        captureTimer = null
        isCaptureInProgress = false
        Log.d("CaptureTimer", "Timer cancelled due to movement/condition break")
    }


















    //Function which creates the circle model to render .The model file is already saved in assets>models folder of project directory in .glb extension file
    private suspend fun makeCircleModel(): ModelNode? {
        return try {
            sceneView.modelLoader.loadModelInstance(faceCircleUri)?.let { modelInstance ->
                ModelNode(
                    modelInstance = modelInstance,
                    scaleToUnits = faceRingConfig.initialRingSize,

                    autoAnimate = true,
                    centerOrigin = null
                ).apply {
                    isEditable = true
                    isTouchable=false

                }
            }
        } catch (e: Exception) {
            Log.e("Model", "Model load failed", e)
            null
        }
    }

    //This function listens to the device’s rotation sensor and calculates the phone’s orientation (yaw, pitch, roll).
    private fun initSensorListener() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return

        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val tempMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(tempMatrix, event.values)

                    // Remap coordinates for portrait orientation
                    SensorManager.remapCoordinateSystem(
                        tempMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        rotationMatrix
                    )

                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    val yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    val normalizedPitch =
                        pitch.toInt() // no need to normalize; pitch already covers up/down

                    // Show camera tilt (vertical movement) — e.g., 0° (straight), -45° (tilted down), +45° (tilted up)
                    angleString = normalizedPitch
                    val cameraAngleText = findViewById<TextView>(R.id.cameraAngle)
                    cameraAngleText.text = angleString.toString();

                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }


    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(cameraId: String, isFrontFacing: Boolean): Int {
        val deviceRotation = windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        rotationCompensation = if (isFrontFacing) {
            (sensorOrientation + rotationCompensation) % 360
        } else {
            (sensorOrientation - rotationCompensation + 360) % 360
        }

        return rotationCompensation
    }


    var isAnyFace = false






    // Function detects the face and updates the face mesh points on face .It also helps user to detect the distance between camera and face
    // Function detects the face and updates the face mesh points on face .It also helps user to detect the distance between camera and face


    // Updates the visual label of distance between camera and face.And helps the user to keep phone close or far
    private fun updateDistanceLabel(baseText: String) {
        distanceLabel.text = "$baseText";
        if (!IsCaptureStarted) {
            distanceLabel.text = "Paused";
            distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_yellow))
        } else {
            when (baseText) {
                "Move Away" -> {

                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_yellow
                        )
                    )
                }

                "Move Closer" -> {

                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_orange
                        )
                    )
                }
                "Subject not in Frame" -> {

                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_orange
                        )
                    )
                }

                "Paused" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_yellow
                        )
                    )
                }

                "Adjust angle" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_yellow
                        )
                    )
                }

                "Subject Detected" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_green_button_light
                        )
                    )
                }

                "Success" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_green_button_light
                        )
                    )
                }

                "Move to Subject`s back" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_cancel_ok
                        )
                    )
                }

                "Hold still" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.round_green_button_light
                        )
                    )
                }

            }

        }

    }





    // Extension function to show the blink effect
    private fun showCaptureBlinkEffect() {
        val overlay = findViewById<View>(R.id.captureOverlay)

        // Cancel any ongoing animation
        overlay.animate().cancel()

        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f

        // Gentle pulse animation
        overlay.animate()
            .alpha(0.4f)  // Only 40% opacity
            .setDuration(80)
            .withEndAction {
                overlay.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction {
                        overlay.visibility = View.INVISIBLE
                    }
            }
    }





    private fun showCompletionDialog() {
        val dialog = Dialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.modal_capture_complete, null))
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setCancelable(false)

            findViewById<TextView>(R.id.captureDoneModalText).text = getString(R.string.captureDoneModalText, referenceNumber,capturedModelList.size.toString())


            findViewById<Button>(R.id.captureOkButton).setOnClickListener {
                navigateToCaptureCompleteScreen()
                dismiss()
            }
        }
        dialog.show()
    }

    private fun navigateToCaptureCompleteScreen() {


        val intent = Intent(this, completeCapture::class.java)
        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
        intent.putExtra("IMAGE_COUNT", capturedModelList.size.toString())
        intent.putExtra("TOTAL_IMAGE_COUNT", nonCapturedModelList.size.toString())

        startActivity(intent)
        finish() // Optional: finish the current activity
    }

    //  The actual function responsible for capturing the camera feed photo and saving it to device .
    private fun captureCameraFeedPhoto(arSceneView: ARSceneView) {
        Log.d("captureCameraFeedPhoto","captureCameraFeedPhoto called")
        faceRing.setBackgroundResource(R.drawable.green_circle)
        val frame = arSceneView.frame ?: return

        try {

            val image = frame.acquireCameraImage() // Get the raw camera image

            // Convert Image to Bitmap
            val bitmap = convertImageToBitmap(image)

            // Save Bitmap to Storage
            val savedUri = saveBitmapToStorage(bitmap)


            Log.d("CapturePhoto", "Photo saved at: $savedUri")

            Toast.makeText(this, "Capture Successful", Toast.LENGTH_SHORT).show()


            image.close() // Release the image

            reconstructionRepository.onImageCaptured()

            // COORDINATES SAVE Camera pose
            val cameraPose = frame.camera.pose
            val right = cameraPose.tx() // X
            val up = cameraPose.ty()    // Y
            val forward = cameraPose.tz() // Z

            val imageInfo = mapOf(
                "image" to "${referenceNumber}_${imageCounter}.jpeg",
                "x" to right,
                "y" to up,
                "z" to forward
            )

            capturedDataList.add(imageInfo)

            saveCoordinatesJsonToStorage()

        } catch (e: Exception) {

            Log.e("CapturePhoto", "Failed to capture photo: ${e.message}")
        }
    }

    //    It saves the world co-ordinates from where the image has been captured to the storage
    private fun saveCoordinatesJsonToStorage() {
        try {
            val root = JSONObject()
            val imagesArray = JSONArray()

            for (entry in capturedDataList) {
                val obj = JSONObject()
                obj.put("image", entry["image"])
                obj.put("x", entry["x"])
                obj.put("y", entry["y"])
                obj.put("z", entry["z"])
                imagesArray.put(obj)
            }

            root.put("Images", imagesArray)

            val jsonString = root.toString(4)
            val fileName = "coordinates.json"
            val imageFolderName = "${referenceNumber}"
            val dir = File(
                Environment.getExternalStorageDirectory(),
                "OpenLIFU-3DScanner/$imageFolderName"
            )
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            file.writeText(jsonString)

            Log.d("JSON_SAVE", "Coordinates saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("JSON_SAVE", "Failed to save coordinates: ${e.message}")
        }
    }




    // An util function which calculates the distance between two points in world coordinates
    private fun calculateDistance(point1: Vector3, point2: Vector3): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        val dz = point1.z - point2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    //function turns a raw camera frame into a Bitmap
    private fun convertImageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height

        val yBuffer = image.planes[0].buffer // Y plane
        val uBuffer = image.planes[1].buffer // U plane
        val vBuffer = image.planes[2].buffer // V plane

        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride

        val nv21 = ByteArray(width * height + 2 * (width / 2) * (height / 2))

        // Copy Y plane
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            yBuffer.get(nv21, row * width, width)
        }

        // Copy UV planes
        val uvHeight = height / 2
        val uvWidth = width / 2
        val uvPlaneSize = uvHeight * uvWidth

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                // Calculate position in NV21 array
                val uvIndex = width * height + row * width + col * 2

                // Handle strides
                val uIndex = row * uvRowStride + col * uvPixelStride
                val vIndex = row * uvRowStride + col * uvPixelStride

                // Swap U and V planes if necessary
                nv21[uvIndex] = vBuffer[vIndex] // V
                nv21[uvIndex + 1] = uBuffer[uIndex] // U
            }
        }

        // Convert NV21 to Bitmap
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val outStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outStream)

        val jpegBytes = outStream.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    //function saves that bitmap as a proper image file on the device.
    private fun saveBitmapToStorage(bitmap: Bitmap) {
        try {
            val fixedBitmap = fixImageRotation(bitmap)
            val filename = "${referenceNumber}_${imageCounter}.jpeg"
            imageCounter++


            val imageFolderName = "${referenceNumber}"
            val rootDir = File(
                Environment.getExternalStorageDirectory(),
                "OpenLIFU-3DScanner/$imageFolderName"
            )
            if (!rootDir.exists()) rootDir.mkdirs()

            val savedFile = File(rootDir, filename)
            FileOutputStream(savedFile).use { fos ->
                fixedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }

            // Optionally scan the file so it appears in gallery immediately
            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(savedFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

            // Add EXIF if needed
            addExifDataForFile(savedFile)

            Log.d("SaveImage", "Image saved at ${savedFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("SaveImage", "Error saving image: ${e.message}")
        }
    }





    //The data from config json file is loaded to the app and stored in a list which is iterated to show models in scene
    private fun loadsDataFromJson() {
        try {
            val jsonFile =
                File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/ringConfig.json")
            if (!jsonFile.exists()) return

            val jsonObject = JSONObject(jsonFile.readText().trim())
            if (!jsonObject.has("bulletCoordinates")) return


            arrowList.clear()   //for scattered bullets

            val arrowCoordinatesArr = jsonObject.getJSONArray("bulletCoordinates")


            //adding scattered bullets objects to a list
            for (i in 0 until arrowCoordinatesArr.length()) {
                val bulletObj = arrowCoordinatesArr.getJSONObject(i)
                arrowList.add(
                    bulletPointConfig(
                        seqID = bulletObj.getInt("seqID"),
                        xPoint = bulletObj.getDouble("xPoint"),
                        yPoint = bulletObj.getDouble("yPoint"),
                        zPoint = bulletObj.getDouble("zPoint"),
                        verticalAngle = bulletObj.getInt("verticalAngle"),
                        horizontalAngle = bulletObj.getInt("horizontalAngle"),

                        )
                )
            }



        } catch (e: Exception) {
            Log.e("loadsDataFromJson", "Error: ${e.message}")
        }
    }



    //The data from config json file is loaded to the app and define where camera will be positiooned
    //The data from config json file is loaded to the app and define where camera will be positiooned
    private fun loadsCameraConfigFromJson() {
        try {
            val configFile = File(
                Environment.getExternalStorageDirectory(),
                "OpenLIFU-Config/ringConfig.json"
            )
            if (!configFile.exists()) {
                Log.e("RingConfig", "Config file not found")
                Toast.makeText(this, "Config file not found!", Toast.LENGTH_LONG).show()
                return
            }

            val root = JSONObject(configFile.readText())

            // Load cameraDistanceForCapture
            root.optJSONObject("cameraDistanceForCapture")?.let { obj ->
                cameraConfigCapture = CameraConfig(
                    minDistance = obj.optDouble("minDistance", 0.32).toFloat(),
                    maxDistance = obj.optDouble("maxDistance",0.34).toFloat()
                )
            }

            // Load cameraDistanceForFaceDetection
            root.optJSONObject("cameraDistanceForFaceDetection")?.let { obj ->
                cameraConfigDetection = CameraConfig(
                    minDistance = obj.optDouble("minDistance", -70.0).toFloat(),
                    maxDistance = obj.optDouble("maxDistance", -68.0).toFloat()
                )
            }

            // Load cubeConfig
            root.optJSONObject("cubeConfig")?.let { obj ->
                cubeConfig = CubeConfig(
                    cubeSize = obj.optDouble("cubeSize", 0.32).toFloat()
                )
            }

            // Load faceRingConfig
            root.optJSONObject("ringAroundFaceConfig")?.let { obj ->
                faceRingConfig = FaceRingConfig(
                    initialRingSize = obj.optDouble("initialRingSize", 0.05).toFloat(),
                    ringScaleFactor = obj.optDouble("ringScaleFactor", 3.2).toFloat()
                )
            }

            // Load cameraRingConfig (safe)
            root.optJSONObject("ringWithCamera")?.let { obj ->
                cameraRingConfig = CameraRingConfig(
                    ringSize = obj.optInt("ringSize", 250)
                )
            }

            root.optJSONObject("cameraCaptureDelayAndSpeed")?.let { obj ->
                maxAllowedSpeed =obj.optDouble("maxAllowedSpeed", 0.6).toFloat()
                delayCaptureBy=obj.optInt("captureDelayTime",1000)

            }

            Log.d("RingConfig", "Configs loaded successfully")

        } catch (e: Exception) {
            Log.e("RingConfig", "Error loading config: ${e.message}")
        }
    }







    //it tags the saved image with basic camera info so that apps reading it (e.g., gallery, photo viewers) see it as if it came from a real camera.
    private fun addExifDataForFile(file: File) {
        try {
            val exif = ExifInterface(file)
            // Add metadata
            exif.setAttribute(ExifInterface.TAG_MAKE, "3D Open Water")
            exif.setAttribute(ExifInterface.TAG_MODEL, "Camera")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "26")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, "26")
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString()
            )
            exif.saveAttributes()

            Log.d("ExifData", "EXIF data saved successfully to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("ExifData", "Failed to save EXIF data for file: ${e.message}")
            Log.e("ExifData", "Failed to save EXIF data for file: ${e.message}")
        }
    }

    private fun fixImageRotation(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f) // Rotate by 90 degrees to fix the orientation
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }




}
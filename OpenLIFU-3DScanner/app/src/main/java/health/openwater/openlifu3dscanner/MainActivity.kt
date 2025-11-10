package health.openwater.openlifu3dscanner

import android.Manifest
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import health.openwater.openlifu3dscanner.Modals.bulletPointConfig
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import com.google.android.filament.Colors
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.material.setColor
import io.github.sceneview.math.Direction
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
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var cloudRepository: CloudRepository

    private companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        const val EXTRA_ALREADY_RESET = "EXTRA_ALREADY_RESET"

    }




    private var cubeNode: ModelNode? = null
    private lateinit var sceneView: ARSceneView
    private lateinit var anchorNode: AnchorNode;
    private lateinit var pivotNode: Node;
    private var pivotBaseQ = Quaternion(0f, 0f, 0f, 1f)
    private var latestNosePosition: Float3? = null
    private var hasVibratedForCenter = false




    val capturedModelList = mutableSetOf<Node>()
    val nonCapturedModelList = mutableListOf<ModelNode>()
    private val faceCircleUri = "models/circle.glb"
    private val faceCubeUri = "models/cube.glb"
    private var activeRing:ModelNode?=null
    private var previousPosition: Vector3? = null
    private var previousTime: Long = 0
    private var lastToastTime: Long = 0
    private val toastCooldown = 2000L

    private lateinit var startButton: Button
    private lateinit var confirmButton: Button
    private lateinit var BackInStartCapture: Button
    private val capturedDataList = mutableListOf<Map<String, Any>>()
    private var currentRingIndex = 0
    private var referenceNumber: String = "DEFAULT_REF"
    private var imageCounter: Int = 1
    private var IsCaptureStarted: Boolean = true
    private lateinit var distanceLabel: TextView
    private lateinit var faceOverlayView: FaceOverlayView
    private val arrowList = mutableListOf<bulletPointConfig>()
    private lateinit var sensorManager: SensorManager
    private lateinit var rotationVectorSensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var angleString = 0
    private lateinit var minAngleText: TextView
    private lateinit var maxAngleText: TextView
    private lateinit var faceRing: ImageView
    private var frameCounter = 0
    private var isFaceDetected = false
    private var hasResetForCurrentFace = false

    private var isFine: Boolean = true


    private var faceCamDistance=0f
    private var pivotUpDownPose=0.131244386f
    private var pivotLeftRightPose= 0.07934329f
    private var pivotCloseFarPose:Float=0f;
    private var globalHorizontalRotaion=0f
    private var globalVerticalRotaion=0f


    private var globalCloseFar:Float=0f;
    private var globalUpDown:Float=0.131244386f;
    private var globalLeftRight:Float=0.07934329f;


    data class CameraConfig(
        val minDistance: Float,
        val maxDistance: Float
    )

    data class CubeOffsets(
        val positionOffsetFine: Float,
        val rotationOffsetFine: Float,
        val positionOffsetCoarse: Float,
        val rotationOffsetCoarse: Float
    )

    data class CubeConfig(
        val cubeSize: Float,
    )

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

    private lateinit var cubeOffset: CubeOffsets
    private var maxAllowedSpeed = 0.6f
    private var delayCaptureBy = 1000
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









    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            // Load arrow data from JSON
            loadDefaultConfigurations()
            loadsconfigurationsFromJson();


            loadsRingArrayFromJson()







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
            val moveBackText = findViewById<TextView>(R.id.MoveBackText)
            val angleContainer = findViewById<ConstraintLayout>(R.id.angleContainer)
            faceRing=findViewById<ImageView>(R.id.faceRing)



            // Find the settings icon
            val controlIcon = findViewById<ImageButton>(R.id.cubeSettingsIcon)

// Find the sliders container
            val allSliders = findViewById<LinearLayout>(R.id.allSliders)

// Set click listener for settings icon to toggle sliders visibility
            controlIcon.setOnClickListener {
                if (allSliders.visibility == View.VISIBLE) {
                    allSliders.visibility = View.GONE

                    startButton.visibility=View.VISIBLE
                    BackInStartCapture.visibility=View.VISIBLE
                    controlIcon.isSelected=false
                    mainScreenTitle.text = "Review AR Box"
                    mainScreenSubTitle.text = "Review the Planned Camera Poses. \n You can adjust the box center by \n tapping the control button."
                } else {
                    allSliders.visibility = View.VISIBLE
                    startButton.visibility=View.GONE
                    BackInStartCapture.visibility=View.GONE
                    controlIcon.isSelected=true
                    mainScreenTitle.text = "Adjust Box Position"
                    mainScreenSubTitle.text = "Use controls to move the box so that it is \n centered on the subject`s head.Tap the \n control button to finish."
                }
            }


            val frontBtn = findViewById<Button>(R.id.frontBtn)

            val BackBtn = findViewById<Button>(R.id.BackBtn)

            val LeftBtn = findViewById<Button>(R.id.LeftBtn)
            val RightBtn = findViewById<Button>(R.id.RightBtn)

            val DownBtn = findViewById<Button>(R.id.DownBtn)
            val UpBtn = findViewById<Button>(R.id.UpBtn)

            val RotateLeftBtn = findViewById<Button>(R.id.RotateLeftBtn)
            val RotateRightBtn = findViewById<Button>(R.id.RotateRightBtn)

            val coarseBtn = findViewById<Button>(R.id.coarseBtn)
            val FineBtn = findViewById<Button>(R.id.FineBtn)
            FineBtn.isSelected=true





            fun Int.toPx(): Int {
                val scale = resources.displayMetrics.density
                return (this * scale + 0.5f).toInt()
            }



            BackBtn.setOnClickListener {

                var newValue=globalCloseFar-(if(isFine) cubeOffset.positionOffsetFine else cubeOffset.positionOffsetCoarse)
                val pivotOffset = Float3(globalUpDown,globalLeftRight,newValue)
                pivotNode.transform(position = pivotOffset)
                globalCloseFar=newValue

            }

            coarseBtn.setOnClickListener {
                isFine=false
                coarseBtn.isSelected=true
                FineBtn.isSelected=false
            }

            FineBtn.setOnClickListener {
                isFine=true
                FineBtn.isSelected=true
                coarseBtn.isSelected=false
            }

            frontBtn.setOnClickListener {

                var newValue = globalCloseFar + (if (isFine) cubeOffset.positionOffsetFine else cubeOffset.positionOffsetCoarse)

                val pivotOffset = Float3(globalUpDown,globalLeftRight,newValue)
                pivotNode.transform(position = pivotOffset)
                globalCloseFar=newValue
            }






            LeftBtn.setOnClickListener {
                var newLR=globalLeftRight-(if(isFine) cubeOffset.positionOffsetFine else cubeOffset.positionOffsetCoarse)

                val pivotOffset = Float3(globalUpDown,newLR,globalCloseFar)
                pivotNode.transform(position = pivotOffset)
                globalLeftRight=newLR
                Log.d("CheckJsonLoadedValues","left -fine-coarse       ${cubeOffset.positionOffsetFine}  ${cubeOffset.positionOffsetCoarse} ")
            }

            RightBtn.setOnClickListener {
                var newLR=globalLeftRight+(if(isFine) cubeOffset.positionOffsetFine else cubeOffset.positionOffsetCoarse)
                Log.d("PoOSSSSSSSSS","${cubeOffset.positionOffsetFine}    ${cubeOffset.positionOffsetCoarse}   ")
                val pivotOffset = Float3(globalUpDown,newLR,globalCloseFar)
                pivotNode.transform(position = pivotOffset)
                globalLeftRight=newLR
            }




            DownBtn.setOnClickListener {
                var newUD=globalUpDown+(if(isFine) cubeOffset.positionOffsetFine else cubeOffset.positionOffsetCoarse)

                val pivotOffset = Float3(newUD,globalLeftRight,globalCloseFar)
                pivotNode.transform(position = pivotOffset)
                globalUpDown=newUD
            }

            UpBtn.setOnClickListener {
                var newUD=globalUpDown-(if(isFine) cubeOffset.positionOffsetFine else cubeOffset.positionOffsetCoarse)


                val pivotOffset = Float3(newUD,globalLeftRight,globalCloseFar)
                pivotNode.transform(position = pivotOffset)
                globalUpDown=newUD
            }



            RotateLeftBtn.setOnClickListener {


                globalHorizontalRotaion=globalHorizontalRotaion-(if(isFine) cubeOffset.rotationOffsetFine else cubeOffset.rotationOffsetCoarse)
                applyVericalRotaion()
            }

            RotateRightBtn.setOnClickListener {
                globalHorizontalRotaion=globalHorizontalRotaion+(if(isFine) cubeOffset.rotationOffsetFine else cubeOffset.rotationOffsetCoarse)
                applyVericalRotaion()
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
                    controlIcon.visibility=View.VISIBLE
                    mainScreenTitle.text = "Review AR Box"
                    mainScreenSubTitle.text = "Review the Planned Camera Poses. \n You can adjust the box center by \n tapping the control button."
                    initialCancelButton.visibility = View.GONE;
                    BackInStartCapture.visibility = View.VISIBLE
                    startButton.visibility = View.VISIBLE;
                    isFaceDetected = true
                    faceOverlayView.visibility = View.GONE
                    distanceLabel.visibility = View.GONE
                    moveBackText.visibility = View.VISIBLE

                }catch (e: Exception){
                    Log.d("ConfirmButtonLogs","Error in confirm button"+e.message.toString())
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

                BackInStartCapture.visibility = View.GONE
                mainScreenTitle.text = "Start Capture"
                mainScreenSubTitle.text = "Position the Subject in the Frame"
                startButton.visibility = View.GONE
                confirmButton.visibility = View.VISIBLE
                controlIcon.visibility=View.GONE
                faceOverlayView.visibility = View.VISIBLE


                // Clear face detection state
                isFaceDetected = false
                faceOverlayView.updatePoints(emptyList(), 0, 0,null)
                if (!isFaceDetected) {
                    confirmButton.isEnabled = false
                }
                initialCancelButton.visibility = View.VISIBLE
                faceOutline.visibility = View.VISIBLE

                currentRingIndex = 0 // Reset ring index
                moveBackText.visibility = View.GONE
                distanceLabel.visibility = View.VISIBLE

                // Reset any other relevant state
                updateDistanceLabel("Subject not in Frame")

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
                    Log.d("LastRing","${nonCapturedModelList.size}")
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
                controlIcon.visibility=View.GONE
                BackInStartCapture.visibility = View.GONE
                stopButton.visibility = View.VISIBLE
                leftArrowInstruction.visibility = View.VISIBLE
                startBlinkingAnimation(leftArrowInstruction)
                leftArrowInstruction.postDelayed({
                    leftArrowInstruction.visibility = View.GONE
                    startTrackingRings();
                    initSensorListener()}, 3000)
            }

            stopButton.setOnClickListener {
                stopButton.visibility = View.GONE
                if (IsCaptureStarted) {
                    IsCaptureStarted = false
                    updateDistanceLabel("Paused");


                }
                endCaptureButton.visibility = View.VISIBLE
                resumeCaptureButton.visibility = View.VISIBLE

            }

            resumeCaptureButton.setOnClickListener {
                if (!IsCaptureStarted) {
                    IsCaptureStarted = true
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


    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }





    private fun applyVericalRotaion() {

        val pitchQ = Quaternion.fromAxisAngle(Direction(1f, 0f, 0f), globalVerticalRotaion)
        val yawQ = Quaternion.fromAxisAngle(Direction(0f, 1f, 0f), globalHorizontalRotaion)

        val finalQ = pivotBaseQ * pitchQ *yawQ
        pivotNode?.transform(quaternion = finalQ)
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

            pivotCloseFarPose=approxFromRange(cameraConfigDetection.minDistance,cameraConfigDetection.minDistance)
            globalCloseFar=pivotCloseFarPose

            //computeHere
            val xPoint =pivotUpDownPose            //up down
            val yPoint =pivotLeftRightPose            //left right
            val zPoint =pivotCloseFarPose










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
        try {{}
            // Keep any orientation you want for the cube
            val rotationX = createQuaternionFromAxisAngle(Vector3(1f, 0f, 0f), 0f)
            val rotationY = createQuaternionFromAxisAngle(Vector3(0f, 1f, 0f), 0f)
            val rotationZ = createQuaternionFromAxisAngle(Vector3(0f, 0f, 1f), 0f)
            val combinedRotation3 = (rotationZ * rotationY * rotationX).toEulerAngles()
            Log.d("CheckJsonLoadedValues","cubeConfig.cubeSize       ${cubeConfig.cubeSize}")
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
                    bulletObj.xPoint.toFloat()-0.06f,
                    bulletObj.yPoint.toFloat()+0.18f,
                    bulletObj.zPoint.toFloat()+0.15f
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



//This function sets up the AR scene by checking if the
// device supports ARCore, configuring features like depth,
// lighting, and plane detection, and hiding plane visuals.


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

    private fun vibrateShort(durationMs: Long = 35) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val v = vm.defaultVibrator
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    v.vibrate(durationMs)
                }
            }
        } catch (_: Exception) { /* safe no-op */ }
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
                bulletNode.isVisible = true
            } else {
                bulletNode.isVisible = false
            }
        }
    }

    //Function which starts the face detection process,it captures every 5th frame of AR frame
    var loggedInitialCamera = false
    private fun startFaceDetection() {
        updateDistanceLabel("Subject not in Frame")

        // Set up frame processing
        sceneView.onSessionUpdated = { session, frame ->
            // Capture initial camera position when tracking starts
            if (!loggedInitialCamera) {
                initialCameraPose = frame.camera.pose
                loggedInitialCamera = true
            }

            checkVisibilityOfBullets()
            frameCounter++
            if (frameCounter % 5 == 0 && !isFaceDetected) {
                processFrameForFaceDetection(frame)
            }
            if (frameCounter > 30) {
                frameCounter = 0
            }
        }
    }


    fun approxFromRange(a: Float, b: Float): Float {
        val lo = minOf(a, b)
        val hi = maxOf(a, b)

        // 1) Exact bucket match (range fully inside a bucket)
        BUCKETS.firstOrNull { lo >= it.start && hi <= it.end }?.let { return it.value }

        // 2) If the midpoint falls inside a bucket, use that bucket’s value
        val mid = (lo + hi) * 0.5f
        BUCKETS.firstOrNull { mid >= it.start && mid <= it.end }?.let { return it.value }

        // 3) Fallback: linear approximation from your samples
        // y ≈ m*x + c  (fit from (-69,-0.5313) to (-36,-0.7413))
        val m = -0.0063636f
        val c = -0.9810f
        val final= m * mid + c
        return final
    }


















    // Converts  the captured frame in bitmap format so that it can be saved in the device using the function [processBitmapForFaceDetection]
    private fun processFrameForFaceDetection(frame: Frame?) {
        try {
            val image = frame?.acquireCameraImage() ?: return
            val bitmap = convertImageToBitmap(image)
            image.close()

            processBitmapForFaceDetection(bitmap)
        } catch (e: Exception) {
            Log.e("FaceDetection", "Error processing frame: ${e.message}")
        }
    }
























    //Function to start tracking the circles around face which helps the  user to capture image according to his desired position
    private fun startTrackingRings() {
        if (nonCapturedModelList.isEmpty()) return
        sceneView.onSessionUpdated = onSessionUpdated@{ _, frame ->
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


    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    /**
     * Map image-space coordinates (ix, iy) to view-space (vx, vy) with a CenterCrop transform.
     * imgW/imgH are the bitmap size passed to the detector.
     * viewW/viewH are the overlay (FaceOverlayView) size on screen.
     */
    private fun imageToViewCoords(
        ix: Float,
        iy: Float,
        imgW: Int,
        imgH: Int,
        viewW: Int,
        viewH: Int
    ): Pair<Float, Float> {
        if (imgW <= 0 || imgH <= 0 || viewW <= 0 || viewH <= 0) return 0f to 0f

        val scale = maxOf(
            viewW.toFloat() / imgW.toFloat(),
            viewH.toFloat() / imgH.toFloat()
        ) // CenterCrop scale

        val drawnW = imgW * scale
        val drawnH = imgH * scale
        val offX = (viewW - drawnW) * 0.5f
        val offY = (viewH - drawnH) * 0.5f

        val vx = ix * scale + offX
        val vy = iy * scale + offY
        return vx to vy
    }

    /** Returns dx, dy in view pixels from view center (dx>0 = nose is right of center; dy>0 = nose is below center). */
    private fun centerOffsetPx(vx: Float, vy: Float, viewW: Int, viewH: Int): Pair<Float, Float> {
        val cx = viewW * 0.5f
        val cy = viewH * 0.5f
        return (vx - cx) to (vy - cy)
    }

    /** True if nose is within tolerance of the screen center. */
    private fun isNoseCentered(dxPx: Float, dyPx: Float, toleranceDp: Float = 24f): Boolean {
        val tolPx = dpToPx(toleranceDp)
        return kotlin.math.abs(dxPx) <= tolPx && kotlin.math.abs(dyPx) <= tolPx
    }




    // Function detects the face and updates the face mesh points on face .It also helps user to detect the distance between camera and face
    private fun processBitmapForFaceDetection(bitmap: Bitmap) {
        try {
            if (isFaceDetected == false) {
                val cameraId = "0" // or "1" for front camera; adjust based on use
                val rotationDegrees = getRotationCompensation(cameraId, isFrontFacing = false)
                val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)

                val options = FaceMeshDetectorOptions.Builder()
                    .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                    .build()

                val detector = FaceMeshDetection.getClient(options)

                detector.process(inputImage)
                    .addOnSuccessListener { faceMeshes ->
                        if (faceMeshes.isNotEmpty()) {
                            val faceMesh = faceMeshes[0]
                            val allPoints = faceMesh.allPoints

                            val nosePose=allPoints.get(1)?.position
                            val noseZ = nosePose?.z ?: 0f






                            // Update the face overlay with mesh information
                            faceOverlayView.updateFaceMesh(
                                faceMesh,
                                bitmap.width,
                                bitmap.height
                            )

                            isAnyFace = true




                            when {
                                noseZ < cameraConfigDetection.minDistance -> {
                                    Log.d(
                                        "isFaceDetected",
                                        "updateDistanceLabel Move Back" + isFaceDetected
                                    )
                                    updateDistanceLabel("Move Away")
                                    confirmButton.isEnabled=false
                                    faceOverlayView.setMeshDetected(false)
                                    hasVibratedForCenter = false
                                }

                                noseZ > cameraConfigDetection.maxDistance -> {
                                    Log.d(
                                        "isFaceDetected",
                                        "updateDistanceLabel MOVE Close" + isFaceDetected
                                    )
                                    updateDistanceLabel("Move Closer")
                                    confirmButton.isEnabled=false
                                    faceOverlayView.setMeshDetected(false)
                                    hasVibratedForCenter = false
                                }

                                else -> {
                                    Log.d("isFaceDetected", "updateDistanceLabel DETECTED" + noseZ)





                                    //checks if camera aligned to nose

                                    val imgW = bitmap.width
                                    val imgH = bitmap.height

// Make sure overlay is measured; if not yet, skip this frame
                                    val vW = faceOverlayView.width
                                    val vH = faceOverlayView.height
                                    if (vW > 0 && vH > 0) {
                                        val (vx, vy) = imageToViewCoords(nosePose?.x ?: 0f,
                                            nosePose?.y ?: 0f, imgW, imgH, vW, vH)
                                        val (dx, dy) = centerOffsetPx(vx, vy, vW, vH)

                                        val centered = isNoseCentered(dx, dy, toleranceDp = 50f)  //24f

                                        // Optional: guidance text for user
                                        val hint = when {
                                            centered -> "Centered"
                                            kotlin.math.abs(dx) > kotlin.math.abs(dy) -> if (dx > 0) "Move camera RIGHT" else "Move camera LEFT"
                                            else -> if (dy > 0) "Move camera DOWN" else "Move camera UP"
                                        }

                                        // Update your existing UI
                                        runOnUiThread {
                                            if (centered) {
                                                updateDistanceLabel("Subject Detected")
                                                confirmButton.isEnabled = true
                                                faceCamDistance = noseZ
                                                faceOverlayView.setMeshDetected(true)

                                                if (!hasVibratedForCenter) {
                                                    vibrateShort(100)
                                                    hasVibratedForCenter = true
                                                }
                                            } else {
                                                faceRing.setBackgroundResource(R.drawable.circle_ring)
                                                updateDistanceLabel("Face Not Aligned")
                                                Log.d("FaceAlignment", "$hint")
                                                faceOverlayView.setMeshDetected(false)
                                                hasVibratedForCenter = false
                                            }
                                        }




                                    }






                                }
                            }
                        } else {
                            faceOverlayView.updatePoints(emptyList(), 0, 0, null)
                            Log.d("isFaceDetected", "updateDistanceLabel DETECTED" + isFaceDetected)
                            updateDistanceLabel("Subject not in Frame")
                            confirmButton.isEnabled = false
                            hasResetForCurrentFace = false
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FaceMeshDetection", "Detection failed: ${e.message}")
                        distanceLabel.text = "Detecting Face..."
                        distanceLabel.setTextColor(
                            ContextCompat.getColor(
                                this,
                                android.R.color.black
                            )
                        )
                        startButton.visibility = View.GONE
                    }
            }
        } catch (e: Exception) {
            Log.e(
                "processBitmapForFaceDetection",
                "Error in processBitmapForFaceDetection: ${e.message}"
            )
            distanceLabel.text = "Detecting Face..."
            distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            startButton.visibility = View.GONE
        }
    }

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



        if (!isFaceDetected) {

            confirmButton.isEnabled = false;
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

            cloudRepository.onImageCaptured()

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
            val filename = "${referenceNumber}_${String.format("%04d", imageCounter)}.jpeg"
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




    private fun loadDefaultRingArray() {
        arrowList.clear()

        arrowList.add(bulletPointConfig(0,  -0.01131267, 0.0,  0.1981966,  24, 0))
        arrowList.add(bulletPointConfig(1,  -0.07311607, 0.0,  0.1884079,  25, 18))
        arrowList.add(bulletPointConfig(2,  -0.12886973, 0.0,  0.16,       24, 36))
        arrowList.add(bulletPointConfig(3,  -0.17311606, 0.0,  0.11575366, 16, 54))
        arrowList.add(bulletPointConfig(4,  -0.20152398, 0.0,  0.06,       16, 72))
        arrowList.add(bulletPointConfig(5,  -0.21131267, 0.0, -0.00180339, 17, 90))
        arrowList.add(bulletPointConfig(6,  -0.20152398, 0.0, -0.06360679, 17, 108))
        arrowList.add(bulletPointConfig(7,  -0.17311606, 0.0, -0.11936044, 15, 126))
        arrowList.add(bulletPointConfig(8,  -0.12886973, 0.0, -0.16360683, 17, 144))
        arrowList.add(bulletPointConfig(9,  -0.07311607, 0.0, -0.19201469, 16, 162))
        arrowList.add(bulletPointConfig(10, -0.01131267, 0.0, -0.20180338, 12, 180))
        arrowList.add(bulletPointConfig(11,  0.050490728, 0.0, -0.19201469, 18, 198))
        arrowList.add(bulletPointConfig(12,  0.106244375, 0.0, -0.16360683, 17, 216))
        arrowList.add(bulletPointConfig(13,  0.15049072,  0.0, -0.11936044, 16, 234))
        arrowList.add(bulletPointConfig(14,  0.17889865,  0.0, -0.06360679, 20, 252))
        arrowList.add(bulletPointConfig(15,  0.18868734,  0.0, -0.00180339, 16, 270))
        arrowList.add(bulletPointConfig(16,  0.17889865,  0.0,  0.06,       27, 288))
        arrowList.add(bulletPointConfig(17,  0.15049072,  0.0,  0.11575366, 25, 306))
        arrowList.add(bulletPointConfig(18,  0.106244375, 0.0,  0.16,       24, 324))
        arrowList.add(bulletPointConfig(19,  0.050490728, 0.0,  0.1884079,  25, 342))


        Log.d("RingArray", "Loaded ${arrowList.size} default ring points")
    }





    //The data from config json file is loaded to the app and stored in a list which is iterated to show models in scene
    private fun loadsRingArrayFromJsonOG() {
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
            Log.e("loadsRingArrayFromJson", "Error: ${e.message}")
        }
    }


    private fun loadsRingArrayFromJson() {
        try {
            val configFile = File(
                Environment.getExternalStorageDirectory(),
                "OpenLIFU-Config/ringConfig.json"
            )

            if (!configFile.exists()) {
                Log.e("RingArray", "File not found — loading default ring data")
                loadDefaultRingArray()
                return
            }

            val content = configFile.readText().trim()
            if (content.isEmpty()) {
                Log.e("RingArray", "Config file empty — loading default ring data")
                loadDefaultRingArray()
                return
            }

            val root = JSONObject(content)
            val arrowArray = root.optJSONArray("bulletCoordinates")

            if (arrowArray == null || arrowArray.length() == 0) {
                Log.e("RingArray", "No bulletCoordinates found — loading default ring data")
                loadDefaultRingArray()
                return
            }

            // Clear previous data
            arrowList.clear()

            for (i in 0 until arrowArray.length()) {
                val obj = arrowArray.optJSONObject(i) ?: continue
                val seqID = obj.optInt("seqID", i)
                val xPoint = obj.optDouble("xPoint", 0.0)
                val yPoint = obj.optDouble("yPoint", 0.0)
                val zPoint = obj.optDouble("zPoint", 0.0)
                val verticalAngle = obj.optInt("verticalAngle", 0)
                val horizontalAngle = obj.optInt("horizontalAngle", 0)

                arrowList.add(
                    bulletPointConfig(
                        seqID = obj.getInt("seqID"),
                        xPoint = obj.getDouble("xPoint"),
                        yPoint = obj.getDouble("yPoint"),
                        zPoint = obj.getDouble("zPoint"),
                        verticalAngle = obj.getInt("verticalAngle"),
                        horizontalAngle = obj.getInt("horizontalAngle"),

                        )
                )
            }

            Log.d("RingArray", "Loaded ${arrowList.size} ring points from JSON")

        } catch (e: Exception) {
            Log.e("RingArray", "Error loading ring array: ${e.message}")
            loadDefaultRingArray()
        }
    }


    private fun loadDefaultConfigurations() {
        cameraConfigCapture = CameraConfig(0.32f, 0.34f)
        cameraConfigDetection = CameraConfig(-70.0f, -68.0f)
        cubeConfig = CubeConfig(0.32f)
        faceRingConfig = FaceRingConfig(0.05f, 3.2f)
        cameraRingConfig = CameraRingConfig(250)
        maxAllowedSpeed = 0.6f
        delayCaptureBy = 1000
        cubeOffset = CubeOffsets(0.01f, 1.0f, 0.05f, 3.0f)

        Log.d("RingConfig", "Loaded default fallback configurations")
    }

    //The data from config json file is loaded to the app and define where camera will be positiooned
    //The data from config json file is loaded to the app and define where camera will be positiooned
    private fun loadsconfigurationsFromJson() {
        try {
            val configFile = File(
                Environment.getExternalStorageDirectory(),
                "OpenLIFU-Config/ringConfig.json"
            )

            if (!configFile.exists()) {
                Log.e("DATALOADFAILED", "file absent ")
                Toast.makeText(this, "Config file not found!", Toast.LENGTH_LONG).show()
                return
            }

            val content = configFile.readText().trim()

            if (content.isEmpty()) {
                Log.e("DATALOADFAILED", "file empty ")
                loadDefaultConfigurations()
                return
            }

            val root = JSONObject(content)

            // load normally as before...
            // (keep all your parsing code here)
            // --- start insertion ---
            try {
                // existing 'root' JSONObject already created above
                // Camera capture distances
                root.optJSONObject("cameraDistanceForCapture")?.let { c ->
                    val minD = c.optDouble("minDistance", cameraConfigCapture.minDistance.toDouble()).toFloat()
                    val maxD = c.optDouble("maxDistance", cameraConfigCapture.maxDistance.toDouble()).toFloat()
                    cameraConfigCapture = CameraConfig(minD, maxD)
                }

                // Face detection distances (your file uses negative ranges)
                root.optJSONObject("cameraDistanceForFaceDetection")?.let { c ->
                    val minD = c.optDouble("minDistance", cameraConfigDetection.minDistance.toDouble()).toFloat()
                    val maxD = c.optDouble("maxDistance", cameraConfigDetection.maxDistance.toDouble()).toFloat()
                    cameraConfigDetection = CameraConfig(minD, maxD)
                }

                // cubeConfig (size)
                root.optJSONObject("cubeConfig")?.let { c ->
                    val cubeSize = c.optDouble("cubeSize", cubeConfig.cubeSize.toDouble()).toFloat()
                    cubeConfig = CubeConfig(cubeSize)
                }

                // ringAroundFaceConfig
                root.optJSONObject("ringAroundFaceConfig")?.let { r ->
                    val initial = r.optDouble("initialRingSize", faceRingConfig.initialRingSize.toDouble()).toFloat()
                    val scale = r.optDouble("ringScaleFactor", faceRingConfig.ringScaleFactor.toDouble()).toFloat()
                    faceRingConfig = FaceRingConfig(initial, scale)
                }

                // ringWithCamera
                root.optJSONObject("ringWithCamera")?.let { rw ->
                    val size = rw.optInt("ringSize", cameraRingConfig.ringSize)
                    cameraRingConfig = CameraRingConfig(size)
                }

                // cameraCaptureDelayAndSpeed
                root.optJSONObject("cameraCaptureDelayAndSpeed")?.let { cs ->
                    maxAllowedSpeed = cs.optDouble("maxAllowedSpeed", maxAllowedSpeed.toDouble()).toFloat()
                    delayCaptureBy = cs.optInt("captureDelayTime", delayCaptureBy)
                }

                // IMPORTANT: parse CubePositonOffsets (this was missing)
                root.optJSONObject("CubePositonOffsets")?.let { co ->
                    val posFine = co.optDouble("positionOffsetFine", cubeOffset.positionOffsetFine.toDouble()).toFloat()
                    val rotFine = co.optDouble("rotationOffsetFine", cubeOffset.rotationOffsetFine.toDouble()).toFloat()
                    val posCoarse = co.optDouble("positionOffsetCoarse", cubeOffset.positionOffsetCoarse.toDouble()).toFloat()
                    val rotCoarse = co.optDouble("rotationOffsetCoarse", cubeOffset.rotationOffsetCoarse.toDouble()).toFloat()

                    cubeOffset = CubeOffsets(posFine, rotFine, posCoarse, rotCoarse)
                }

                // load bulletCoordinates as before (you already have loadsRingArrayFromJson() but ensure arrowList loaded)
                loadsRingArrayFromJson()

                Log.d("RingConfigLoaded", "cameraCapture=${cameraConfigCapture}, cameraDetect=${cameraConfigDetection}, cubeOffset=${cubeOffset}, cubeSize=${cubeConfig.cubeSize}")
            } catch (e: Exception) {
                Log.e("DATALOADFAILED", "Error parsing config keys: ${e.message}")
                loadDefaultConfigurations()
            }
// --- end insertion ---


        } catch (e: Exception) {
            Log.e("DATALOADFAILED", "Error loading config: ${e.message}")
            Toast.makeText(this, "Invalid config file!", Toast.LENGTH_LONG).show()
            loadDefaultConfigurations() // fallback
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
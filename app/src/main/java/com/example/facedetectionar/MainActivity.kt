package com.example.facedetectionar

import android.Manifest
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Camera
import android.graphics.ImageFormat
import android.graphics.PointF
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.sqrt
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.SparseIntArray
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.collection.emptyLongSet
import androidx.compose.ui.window.Dialog
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import java.util.EnumSet
import androidx.exifinterface.media.ExifInterface
import com.example.facedetectionar.Modals.ArcConfig
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark


import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import org.json.JSONArray
import org.json.JSONObject


class CustomArFragment : ArFragment() {

}

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var imageCountText: TextView
    private lateinit var startButton: Button
    private lateinit var confirm_button:Button
    private lateinit var BackInStartCapture:Button
    private val capturedDataList = mutableListOf<Map<String, Any>>()
    private lateinit var grayMaterial: Material
    private lateinit var greenMaterial: Material
    private val ringNodes = mutableListOf<MutableList<AnchorNode>>()
    private var currentRingIndex = 0


    private val bulletNodes = mutableListOf<AnchorNode>()
    private var closestBulletNode: AnchorNode? = null // Track closest bullet node
    private var referenceNumber: String = "DEFAULT_REF" // Default reference number
    private var imageCounter: Int = 1
    private var IsCaptureStarted: Boolean = true
    private lateinit var distanceLabel: TextView
    private lateinit var faceOverlayView: FaceOverlayView

    private val bulletConfigList = mutableListOf<ArcConfig>()


    private lateinit var sensorManager: SensorManager
    private lateinit var rotationVectorSensor: Sensor
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var angleString = ""





    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)


            // Load bullet config from JSON
            loadsDataFromJson()

            // Initialize UI components
            distanceLabel = findViewById(R.id.distanceLabel)
            arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
            val leftArrowInstruction = findViewById<TextView>(R.id.leftArrowInstruction)
            startButton = findViewById(R.id.startButton)
            val intialRenderCamCancel=findViewById<Button>(R.id.intialRenderCamCancel)
            val confirm_button=findViewById<Button>(R.id.confirm_button)
            val BackInStartCapture=findViewById<Button>(R.id.BackInStartCapture)
            val stopButton=findViewById<Button>(R.id.stopButton)
            val endCapture_button=findViewById<Button>(R.id.endCapture_button)
            val resumeCapture_button=findViewById<Button>(R.id.resumeCapture_button)
            val mainScreenTitle=findViewById<TextView>(R.id.mainScreenTitle)
            val mainScreenSubTitle=findViewById<TextView>(R.id.mainScreenSubTitle)
            val imageCountText=findViewById<TextView>(R.id.imageCountText)
            val cameraAngleText=findViewById<TextView>(R.id.cameraAngle)
            cameraAngleText.text=angleString;



            // Set the reference number from the previous screen
            referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "DEFAULT_REF"
            //LogFileUtil.appendLog("Reference number set: $referenceNumber")













            // Disable AR plane detection
            //LogFileUtil.appendLog("Disabling plane detection and visual elements")
            arFragment.planeDiscoveryController.hide()
            arFragment.planeDiscoveryController.setInstructionView(null)
            arFragment.arSceneView.planeRenderer.isVisible = false

            // Configure AR session
            val session = try {
                //LogFileUtil.appendLog("Initializing AR session")
                Session(this)
            } catch (e: Exception) {
                //LogFileUtil.appendLog("Failed to initialize AR session: ${e.message}")
                Log.d("AR session initialization failed", e.message.toString())
                Toast.makeText(this, "AR session initialization failed", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val config = Config(session).apply {
                focusMode = Config.FocusMode.AUTO
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                depthMode=Config.DepthMode.AUTOMATIC
            }

            try {

                //LogFileUtil.appendLog("Configuring AR session")
                session.configure(config)
                arFragment.arSceneView.setupSession(session)
                //LogFileUtil.appendLog("AR session configured successfully")
            } catch (e: Exception) {
                //LogFileUtil.appendLog("AR session configuration failed: ${e.message}")
                Toast.makeText(this, "AR session configuration failed", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            confirm_button.setOnClickListener {
                confirm_button.visibility=View.GONE;
                mainScreenSubTitle.text="Review the Planned Camera Poses"
                intialRenderCamCancel.visibility=View.GONE;
                BackInStartCapture.visibility=View.VISIBLE
                startButton.visibility=View.VISIBLE;
            }

            BackInStartCapture.setOnClickListener {

                BackInStartCapture.visibility=View.GONE
                mainScreenSubTitle.text="Position the Subject in the Frame"
                startButton.visibility=View.GONE;
                confirm_button.visibility=View.VISIBLE;
                if(!isFaceDetected){
                    confirm_button.isEnabled=false;
                }

                intialRenderCamCancel.visibility=View.VISIBLE;

            }

            stopButton.setOnClickListener {
                stopButton.visibility=View.GONE
                if (IsCaptureStarted) {
                    //stops capturing
                    IsCaptureStarted = false
                    updateDistanceLabel("Paused");


                }
                endCapture_button.visibility=View.VISIBLE
                resumeCapture_button.visibility=View.VISIBLE

            }

            resumeCapture_button.setOnClickListener {
                if(!IsCaptureStarted){
                    IsCaptureStarted = true
                }
                resumeCapture_button.visibility=View.GONE
                endCapture_button.visibility=View.GONE
                stopButton.visibility=View.VISIBLE;


            }

            endCapture_button.setOnClickListener {
                val dialog = android.app.Dialog(this)

                val view=layoutInflater.inflate(R.layout.modal_capture_end,null)
                val noButton=view.findViewById<Button>(R.id.endCaptureNoBtn)

                val imageCount = greenBulletList.size // addding image count to modal
                val text = getString(R.string.endcaptureText, imageCount)
                val endText=view.findViewById<TextView>(R.id.endCaptureTextLabel)
                endText.text = text

                val yesButton=view.findViewById<Button>(R.id.endCaptureYesBtn)

                noButton.setOnClickListener {
                    dialog.dismiss()
                    if(!IsCaptureStarted){
                        IsCaptureStarted = true
                    }
                    resumeCapture_button.visibility=View.GONE
                    endCapture_button.visibility=View.GONE
                    stopButton.visibility=View.VISIBLE;


                }

                yesButton.setOnClickListener {
                    if(greenBulletList.size==0){
                        val intent= Intent(this, New_capture::class.java)
                        startActivity(intent)
                        finish()
                    }else{
                        val intent= Intent(this,completeCapture::class.java)
                        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
                        intent.putExtra("IMAGE_COUNT",greenBulletList.size.toString())
                        intent.putExtra("TOTAL_IMAGE_COUNT",bulletNodes.size.toString())
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



            intialRenderCamCancel.setOnClickListener {
                val intent = Intent(this, New_capture::class.java)

                startActivity(intent)
                finish()

            }

            // Pre-render materials and then initialize bullets
            preRenderMaterials {
                //LogFileUtil.appendLog("Materials pre-rendered successfully")
                startButton.setOnClickListener {
                    //LogFileUtil.appendLog("Start button clicked: initializing bullets")

                    placeDynamicBulletsAtCameraFocusFlat() // Place the ring dynamically
                    mainScreenTitle.text="Capture"
                    mainScreenSubTitle.visibility= View.GONE
                    imageCountText.visibility= View.VISIBLE
                    startButton.visibility = View.GONE // Hide button after placement
                    BackInStartCapture.visibility=View.GONE
                    stopButton.visibility = View.VISIBLE



                    //LogFileUtil.appendLog("Logcat Capture stopped")
                    // Stop capturing Logcat output
                    //LogFileUtil.stopLogcatCapture()



                    // Show the instruction
                    leftArrowInstruction.visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.targetCircle).visibility = View.VISIBLE
                    // Start the blinking animation
                    startBlinkingAnimation(leftArrowInstruction)

                    // Hide it after 3 seconds
                    leftArrowInstruction.postDelayed({
                        leftArrowInstruction.visibility = View.GONE
                        startBulletTracking() // Start tracking bullets
                        initSensorListener() //verify camera alignment using angles

//
                    }, 3000) // 3000 milliseconds = 3 seconds

                }






            }

            // Check for camera permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //LogFileUtil.appendLog("Camera permission not granted: requesting permissions")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            } else {
                //LogFileUtil.appendLog("Camera permission already granted")
            }

            val overlayContainer = findViewById<FrameLayout>(R.id.overlayContainer)
            faceOverlayView = FaceOverlayView(this)
            overlayContainer.addView(faceOverlayView)

            startFaceDetection()

            //LogFileUtil.appendLog("AR screen setup completed successfully")
        } catch (e: Exception) {
            //LogFileUtil.appendLog("Exception in onCreate: ${e.message}")
            e.printStackTrace()
        }










    }























//    ALL Functions from here




    private fun startBlinkingAnimation(textView: TextView) {
        val animator = ValueAnimator.ofFloat(1f, 0f, 1f) // From fully visible to invisible to visible
        animator.duration = 1000 // 500ms per blink
        animator.repeatCount = ValueAnimator.INFINITE // Keep repeating
        animator.addUpdateListener { animation ->
            val alphaValue = animation.animatedValue as Float
            textView.alpha = alphaValue // Set alpha value for blinking effect
            textView.elevation = 10 * (1 - alphaValue) // Simulate shadow elevation
        }
        animator.start()
    }
    private var frameCounter = 0
    private var isFaceDetected = false
    private fun startFaceDetection() {
        //LogFileUtil.appendLog("AR screen: face detection started")

        // Initially hide start button and targetCircle

        findViewById<ImageView>(R.id.targetCircle).visibility = View.GONE
        distanceLabel.text = "No subject Detected"
        distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_orange))

        arFragment.arSceneView.scene.addOnUpdateListener {
            checkVisibilityOfBullets()
            frameCounter++
            if (frameCounter == 5 && !isFaceDetected) { // Process every 5th frame
                try {
                    val frame: Frame = arFragment.arSceneView.arFrame ?: return@addOnUpdateListener
                    val cameraImage = frame.acquireCameraImage()

                    // Process face detection
                    val bitmapImage = convertImageToBitmap(cameraImage)

                    processBitmapForFaceDetection(bitmapImage)

                    cameraImage.close()
                    frameCounter = 0
                } catch (e: NotYetAvailableException) {
                    Log.e("FaceDetection", "No image available for this frame: ${e.message}")
                } catch (e: Exception) {
                    Log.e("FaceDetection", "Unexpected error: ${e.message}")
                }
            }
            if(frameCounter > 30)
            {
                frameCounter = 0
            }
        }
    }


    //to hide bullets behind head
    private fun checkVisibilityOfBullets() {
        val frame = arFragment.arSceneView.arFrame ?: return
        val cameraPose = frame.camera.pose
        val cameraPosition = Vector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
        val cameraForward = Vector3(-cameraPose.zAxis[0], -cameraPose.zAxis[1], -cameraPose.zAxis[2]).normalized()

        for (bulletNode in bulletNodes) {
            val bulletPosition = bulletNode.worldPosition
            val vectorToBullet = Vector3.subtract(bulletPosition, cameraPosition)
            val distance = vectorToBullet.length()

            val directionToBullet = vectorToBullet.normalized()
            val dot = Vector3.dot(cameraForward, directionToBullet) // 1 = directly facing, -1 = away
            Log.d("Bullet Distance","Bullet Distance: ${distance}, name: (${bulletNode.name})")
            // Visibility logic
            val inGoodDistance = distance in 0.2f..0.4f
            val isFacing = dot > 0.6f  // Adjust as needed

//            bulletNode.isEnabled = inGoodDistance && isFacing

            if(distance > 0.6f && distance < 0.8f)
            {
                bulletNode.isEnabled = false
            }
            else
            {
                bulletNode.isEnabled = true
            }
        }
    }

    private fun preRenderMaterials(onComplete: () -> Unit) {
        try{
            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(android.graphics.Color.GRAY))
                .thenAccept { material ->
                    grayMaterial = material
                    checkMaterialsReady(onComplete)
                }

            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(android.graphics.Color.GREEN))
                .thenAccept { material ->
                    greenMaterial = material
                    checkMaterialsReady(onComplete)
                }
        }
        catch (e: Exception)
        {
            //LogFileUtil.appendLog("AR screen preRenderMaterials: ${e.message}")
        }
    }

    private fun checkMaterialsReady(onComplete: () -> Unit) {
        if (::grayMaterial.isInitialized && ::greenMaterial.isInitialized) {
            onComplete()
        }
    }

    private fun getFacePositionFromDetection(): PointF {
        // This should return the center position of the detected face
        // in normalized screen coordinates (0-1)

        // Example implementation:
        return PointF(0.5f, 0.5f) // Center of screen

        // In your actual code, you should use the face detection results
        // from your face mesh detection to get the actual face position
    }

    private fun placeRingAroundFaceWithDepth() {
        val frame = arFragment.arSceneView.arFrame ?: return
        val depthImage =  frame.acquireDepthImage16Bits() ?: run {
            Log.w("DepthAPI", "No depth image available")
            return
        }

        try {
            // Get face position from your face detection
            val facePosition = getFacePositionFromDetection() ?: return

            // Get depth at face position
            val depthValue = getDepthAtPosition(depthImage, facePosition)

            if (depthValue <= 0f) {
                Log.w("DepthAPI", "Invalid depth value")
                return
            }

            // Calculate position in front of the face
            val cameraPose = frame.camera.pose
            val cameraForward = floatArrayOf(0f, 0f, -1f) // Camera's forward vector
            val distanceInFrontOfFace = 0.05f // 5cm in front of face

            // Calculate world position for the ring
            val ringPosition = floatArrayOf(
                facePosition.x,
                facePosition.y,
                -(depthValue + distanceInFrontOfFace)
            )

            // Transform to world coordinates
            val worldPosition = cameraPose.transformPoint(ringPosition)

            // Create anchor at this position
            val session = arFragment.arSceneView.session ?: return
            val anchor = session.createAnchor(Pose(worldPosition, floatArrayOf(0f, 0f, 0f, 1f)))

            // Create AnchorNode from the Anchor
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            // Create the ring
            createBulletRing(anchorNode)

        } finally {
            depthImage.close()
        }
    }

    private fun createBulletRing(anchorNode: AnchorNode) {
        val radius = 0.18f // Radius of the ring
        val bulletCount = 70

        bulletNodes.clear() // Clear previous bullets

        for (i in 0 until bulletCount) {
            val angle = 2 * Math.PI * i / bulletCount - Math.PI / -2
            val x = (radius * kotlin.math.cos(angle)).toFloat()
            val z = (radius * kotlin.math.sin(angle)).toFloat()

            val position = Vector3(x, 0f, z)
            val bulletNode = AnchorNode()
            bulletNode.setParent(anchorNode)
            bulletNode.localPosition = position

            bulletNodes.add(bulletNode)

            bulletNode.renderable = ShapeFactory.makeSphere(0.007f, Vector3.zero(), grayMaterial).apply {
                isShadowCaster = false
                isShadowReceiver = false
            }
        }

        // Add vertical bullets (same as before)
//        addVerticalBullets(anchorNode)
    }

    private fun getDepthAtPosition(depthImage: Image, position: PointF): Float {
        val depthBuffer = depthImage.planes[0].buffer
        val depthWidth = depthImage.width
        val depthHeight = depthImage.height

        // Convert normalized position to depth image coordinates
        val u = (position.x * depthWidth).toInt().coerceIn(0, depthWidth - 1)
        val v = (position.y * depthHeight).toInt().coerceIn(0, depthHeight - 1)

        // Get depth value (in millimeters)
        val depthValue = depthBuffer.getShort(v * depthWidth + u).toFloat()

        // Convert to meters
        return depthValue / 1000f
    }

    private fun placeDynamicBulletsAtCameraFocusFlat() {
        try {
            val frame: Frame = arFragment.arSceneView.arFrame ?: return
            val cameraPose: Pose = frame.camera.pose

            if (bulletConfigList.isNotEmpty()) {
                bulletConfigList.forEachIndexed { index, arc ->
                    val ringBullets = mutableListOf<AnchorNode>()  // NEW: Store bullets for this ring


                    Log.d("placeDynamicBulletsAtCameraFocusFlat","Bullet Config$index:Count: ${arc.bulletCount} ,Up down: ${arc.upDown.toFloat()}")

                    // --- YOUR EXISTING BULLET PLACEMENT CODE ---
                    val upDownValue = arc.upDown.toFloat()
                    val closeFarValue = arc.closeFar.toFloat()
                    val arcRadius = arc.radius.toFloat()
                    val forwardVector = floatArrayOf(upDownValue, 0f, closeFarValue)
                    val cameraPosition = cameraPose.transformPoint(forwardVector)
                    val flatOrientation = floatArrayOf(0f, 0f, 0f, 1f)
                    val session = arFragment.arSceneView.session ?: return
                    val anchor = session.createAnchor(Pose(cameraPosition, flatOrientation))
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(arFragment.arSceneView.scene)

                    val bulletCount = arc.bulletCount
                    for (i in 0 until bulletCount) {
                        val angle = 2 * Math.PI * i / bulletCount - Math.PI / -2
                        val x = (arcRadius * kotlin.math.cos(angle)).toFloat()
                        val z = (arcRadius * kotlin.math.sin(angle)).toFloat()
                        val position = Vector3(x, 0f, z)
                        val bulletNode = AnchorNode().apply {
                            setParent(anchorNode)
                            localPosition = position
                            renderable = ShapeFactory.makeSphere(0.007f, Vector3.zero(), grayMaterial).apply {
                                isShadowCaster = false
                                isShadowReceiver = false
                            }
                            // NEW: Enable bullets in the first ring initially
                            isEnabled = (index == 0)  // Only enable first ring
                        }
                        ringBullets.add(bulletNode)
                        bulletNodes.add(bulletNode)  // Preserve your original flat list
                    }


                    ringNodes.add(ringBullets)  // NEW: Add this ring to the grouped list
                }
            }
        } catch (e: Exception) {
            Log.e("BulletPlacement", "Error placing bullets: ${e.message}")
        }
    }


    private fun addBulletIdText(parentNode: AnchorNode, bulletId: Int) {
        ViewRenderable.builder()
            .setView(this, R.layout.bullet_id_text) // Layout for bullet ID text
            .build()
            .thenAccept { viewRenderable ->
                val textView = viewRenderable.view as TextView
                textView.text = bulletId.toString()

                val textNode = AnchorNode()
                textNode.setParent(parentNode)
                textNode.localPosition = Vector3(0f, 0.02f, 0f) // Adjust text position above bullet
                textNode.renderable = viewRenderable
            }
            .exceptionally { throwable ->
                Log.e("MainActivity", "Failed to create text renderable: ${throwable.message}")
                null
            }
    }

    private fun startBulletTracking() {
        var skipFrame = 0
        arFragment.arSceneView.scene.addOnUpdateListener {
            val frame: Frame = arFragment.arSceneView.arFrame ?: return@addOnUpdateListener
            val cameraPose = frame.camera.pose
            val cameraPosition = Vector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())
            skipFrame++;
            var closestNode: AnchorNode? = null
            // Only track bullets from current active ring
            if (currentRingIndex < ringNodes.size) {
                val currentRing = ringNodes[currentRingIndex]
                var closestDistance = Float.MAX_VALUE


                currentRing.forEach { bulletNode ->
                    if (!greenBulletList.contains(bulletNode)) { // Only consider non-green bullet and active ring bullets
                        val bulletPosition = bulletNode.worldPosition
                        val distance = calculateDistance(cameraPosition, bulletPosition)

                        Log.d("startBulletTracking","closestDistance: ${closestDistance}")

                        if (distance < closestDistance) { // Find the closest bullet
                            closestDistance = distance


                            // Update the distance label based on the distance
                            when {
                                distance < 0.2f -> {
                                    updateDistanceLabel("Too close")
                                }

                                distance > 0.4f -> {
                                    updateDistanceLabel("Move Closure")

                                }

                                distance > 0.2f && distance < 0.4f-> {

                                    closestNode = bulletNode
                                    updateDistanceLabel("Move to next Position")
                                    if (skipFrame > 10 && IsCaptureStarted) {
                                        // Update bullet colors
                                        Log.d("startBulletTracking","skip frame count"+skipFrame)
                                        Log.d("startBulletTracking","Bullet Distance: ${distance}")
                                        updateBulletColors(closestNode,distance)
                                    }
                                }

                            }
                        }
                    }

                }



            }
        }
    }




    private fun initSensorListener() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

                    val normalizedPitch = pitch.toInt() // no need to normalize; pitch already covers up/down

                    // Show camera tilt (vertical movement) — e.g., 0° (straight), -45° (tilted down), +45° (tilted up)
                    angleString = "CameraTilt: ${normalizedPitch}°"
                    Log.d("CameraTilt","CameraTilt"+normalizedPitch);
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

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
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

    var progressCounter = 0
    var isAnyFace = false
    private fun processBitmapForFaceDetection(bitmap: Bitmap) {


        try {
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
                        val boundingBox = faceMesh.boundingBox

                        val allPoints = faceMesh.allPoints

                        val noseZ = allPoints.get(1)?.position?.z ?: 0f
                        val noseX = allPoints.get(1)?.position?.x ?: 0f
                        val noseY = allPoints.get(1)?.position?.y ?: 0f
                        //val foreheadZ = allPoints.get(10)?.position?.z ?: 0f
                        //val chinZ = allPoints.get(152)?.position?.z ?: 0f
                        //val avgZ = (noseZ + foreheadZ + chinZ) / 3f


                        Log.d("NoseCoordinates", "X: $noseX, Y: $noseY, Z: $noseZ")

                        faceOverlayView.updatePoints(faceMesh.allPoints, bitmap.width, bitmap.height)

                        isAnyFace = true

                        val faceWidth = boundingBox.width()
                        val faceHeight = boundingBox.height()

                        // Apply logic based on face size + z-depth
                        when {
                            noseZ < -75.0 -> { // Too close (wide and shallow depth)
                                // if (!isFaceDetected) updateDistanceLabel("Move Away") while initial subject detectiion

                            }
                            noseZ > -60.0 -> { // Too far (small face and deeper z)
                                //   if (!isFaceDetected) updateDistanceLabel("Move Closure") while initial subject detectiion


                            }
                            else -> {
                                //isFaceDetected = true
                                distanceLabel.text = "Subject Detected"
                                confirm_button=findViewById<Button>(R.id.confirm_button)
                                confirm_button.isEnabled=true;


                                distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                                distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_green_button_light))
//                                startButton.visibility = View.VISIBLE
//                                faceOverlayView.updatePoints(faceMesh.allPoints, bitmap.width, bitmap.height)
                            }
                        }
                    } else {
//                        if (!isFaceDetected && !isAnyFace) updateDistanceLabel("No subject detected")
                        faceOverlayView.updatePoints(emptyList(), bitmap.width, bitmap.height)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceMeshDetection", "Detection failed: ${e.message}")
                    distanceLabel.text = "Detecting Face..."
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                    startButton.visibility = View.GONE
                    findViewById<ImageView>(R.id.targetCircle).visibility = View.GONE
                }
        } catch (e: Exception) {
            Log.e("FaceMeshDetection", "Error: ${e.message}")
            distanceLabel.text = "Detecting Face..."
            distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            startButton.visibility = View.GONE
            findViewById<ImageView>(R.id.targetCircle).visibility = View.GONE
        }
    }


    private fun updateDistanceLabel(baseText: String) {
        when (progressCounter) {
            0 -> distanceLabel.text = "$baseText"
            1 -> distanceLabel.text = "$baseText."
            2 -> distanceLabel.text = "$baseText.."
            3 -> {
                distanceLabel.text = "$baseText..."
                progressCounter = -1
            }
        }
        progressCounter++



        if(!IsCaptureStarted){
            distanceLabel.text="Paused";
            distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_yellow))
        }else{
            when (baseText) {
                "Too close" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_orange))
                }
                "Move to next Position" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_cancel_ok))
                }
                "Move Closure" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_orange))
                }
                "No subject detected" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                }
                "Paused" -> {
                    distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    distanceLabel.setBackground(ContextCompat.getDrawable(this, R.drawable.round_yellow))
                }

            }

        }



        if(!isFaceDetected){
            confirm_button=findViewById<Button>(R.id.confirm_button)
            confirm_button.isEnabled=false;
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




    val greenBulletList = mutableSetOf<AnchorNode>() // Ensures no duplicate entries



    private fun updateBulletColors(closestNode: AnchorNode?,distance: Float) {


        if (closestNode != null && !greenBulletList.contains(closestNode)) {

            showCaptureBlinkEffect()

            //make bullets green
            changeBulletToGreen(closestNode)

             // keep record of number of images
            greenBulletList.add(closestNode)

            // Trigger vibration (your existing code)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
            Log.d("updateBulletColors", "ditance at capture time"+distance)
            captureCameraFeedPhoto(arFragment.arSceneView)


            // Check if current ring is complete and enable other ring
            if (currentRingIndex < ringNodes.size) {
                val currentRing = ringNodes[currentRingIndex]
                val currentRingComplete = currentRing.all { greenBulletList.contains(it) }

                if (currentRingComplete) {
                    // Move to next ring if available
                    if (currentRingIndex < ringNodes.size - 1) {
                        currentRingIndex++
                        // Enable next ring's bullets
                        ringNodes[currentRingIndex].forEach { it.isEnabled = true }
                        Toast.makeText(this, "Starting next ring capture", Toast.LENGTH_SHORT).show()
                    } else {
                        // All rings complete
                        showCompletionDialog()
                    }
                }
            }




            // Preserve your original counter logic
            val imageCountText = findViewById<TextView>(R.id.imageCountText)
            imageCountText.text = "${greenBulletList.size}/${bulletNodes.size}"
        }




    }

    private fun showCompletionDialog() {
        val dialog = Dialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.modal_capture_complete, null))
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setCancelable(false)

            findViewById<TextView>(R.id.captureDoneModalText).text =
                getString(R.string.captureDoneModalText, referenceNumber,greenBulletList.size.toString())

            findViewById<Button>(R.id.captureOkButton).setOnClickListener {
                navigateToCaptureCompleteScreen()
                dismiss()
            }
        }
        dialog.show()
    }

    private fun navigateToCaptureCompleteScreen() {

        //LogFileUtil.appendLog("Transferring to Image transfer screen, connect usb")
        val intent = Intent(this, completeCapture::class.java)
        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
        intent.putExtra("IMAGE_COUNT", greenBulletList.size.toString())
        intent.putExtra("TOTAL_IMAGE_COUNT", bulletNodes.size.toString())

        startActivity(intent)
        finish() // Optional: finish the current activity
    }
















    private fun captureCameraFeedPhoto(arSceneView: ArSceneView) {
        val frame = arSceneView.arFrame ?: return

        try {
            val image = frame.acquireCameraImage() // Get the raw camera image

            // Convert Image to Bitmap
            val bitmap = convertImageToBitmap(image)

            // Save Bitmap to Storage
            val savedUri = saveBitmapToStorage(bitmap)

            //LogFileUtil.appendLog("AR screen: step 10: Photo saved")
            Log.d("CapturePhoto", "Photo saved at: $savedUri")
            //Toast.makeText(arSceneView.context, "Photo saved at: $savedUri", Toast.LENGTH_SHORT).show()

            image.close() // Release the image


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
            //LogFileUtil.appendLog("AR screen: Failed to capture photo: ${e.message}")
            Log.e("CapturePhoto", "Failed to capture photo: ${e.message}")
        }
    }


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
            val imageFolderName="${referenceNumber}_${getTotalImageCountFromJson()}"
            val dir = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner/$imageFolderName")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, fileName)
            file.writeText(jsonString)

            Log.d("JSON_SAVE", "Coordinates saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("JSON_SAVE", "Failed to save coordinates: ${e.message}")
        }
    }


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



    private  fun  getTotalImageCountFromJson(): Int {
        try {
            Log.d("loadsDataFromJson", "try started")
            val jsonFile = File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/ringConfig.json")

            if (!jsonFile.exists()) {
                Log.e("loadsDataFromJson", "ringConfig.json file not found")
                return 0
            }

            val jsonContent = jsonFile.readText().trim()  // Trim whitespace

            if (jsonContent.isEmpty()) {
                Log.w("loadsDataFromJson", "JSON file is empty")
                return 0
            }

            val jsonObject = JSONObject(jsonContent)

            // Check if "arcs" key exists and is an array
            if (!jsonObject.has("arcs") || jsonObject.isNull("arcs")) {
                Log.w("loadsDataFromJson", "No 'arcs' array in JSON")
                return 0
            }

            val arcsArray = jsonObject.getJSONArray("arcs")

            if (arcsArray.length() == 0) {
                Log.w("loadsDataFromJson", "'arcs' array is empty")
                return 0
            }

          var sumOfBulletCounts=0;


            for (i in 0 until arcsArray.length()) {
                val arcObject = arcsArray.getJSONObject(i)
                val bulletCount = arcObject.getInt("bulletCount")
                sumOfBulletCounts+=bulletCount;

            }
            return sumOfBulletCounts;

        } catch (e: Exception) {
            Log.e("loadsDataFromJson", "Error parsing JSON: ${e.message}")
        }


        return 0
    }






    private fun saveBitmapToStorage(bitmap: Bitmap) {
        try {
            val fixedBitmap = fixImageRotation(bitmap)
            val filename = "${referenceNumber}_${imageCounter}.jpeg"
            imageCounter++

            // Define root path directory: /storage/emulated/0/OpenLIFU-3DScanner/{referenceNumber}/
            val totalImages=getTotalImageCountFromJson()
            val imageFolderName="${referenceNumber}_${totalImages}"
            val rootDir = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner/$imageFolderName")
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




    private fun loadsDataFromJson() {
        try {
            val jsonFile = File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/ringConfig.json")
            if (!jsonFile.exists()) return

            val jsonObject = JSONObject(jsonFile.readText().trim())
            if (!jsonObject.has("arcs")) return

            bulletConfigList.clear()
            val arcsArray = jsonObject.getJSONArray("arcs")

            for (i in 0 until arcsArray.length()) {
                val arcObject = arcsArray.getJSONObject(i)
                bulletConfigList.add(ArcConfig(
                    type = arcObject.getString("type"),
                    radius = arcObject.getDouble("radius"),
                    bulletCount = arcObject.getInt("bulletCount"),
                    upDown = arcObject.getDouble("upDown"),
                    closeFar = arcObject.getDouble("closeFar")
                ))
            }

            // NEW: Sort rings by height (ascending order)
            bulletConfigList.sortBy { it.upDown }
            Log.d("RingSorting", "Sorted rings: ${bulletConfigList.map { it.upDown }}")

        } catch (e: Exception) {
            Log.e("loadsDataFromJson", "Error: ${e.message}")
        }
    }




    private fun addExifDataForUri(uri: Uri) {
        try {
            val resolver = applicationContext.contentResolver
            resolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File.createTempFile("temp_image", ".jpeg", cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                val exif = ExifInterface(tempFile)
                // Add metadata
                exif.setAttribute(ExifInterface.TAG_MAKE, "3D Open Water")
                exif.setAttribute(ExifInterface.TAG_MODEL, "Camera")
                exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "26")
                exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, "26")
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                exif.saveAttributes()

                // Write modified file back to original URI
                resolver.openOutputStream(uri)?.use { outputStream ->
                    tempFile.inputStream().use { it.copyTo(outputStream) }
                }

                tempFile.delete() // Clean up temporary file
            }
            Log.d("ExifData", "EXIF data saved successfully to $uri")
        } catch (e: Exception) {
            //LogFileUtil.appendLog("AR screen: step 11, Failed to save EXIF data for URI: ${e.message}")
            Log.e("ExifData", "Failed to save EXIF data for URI: ${e.message}")
        }
    }

    private fun addExifDataForFile(file: File) {
        try {
            val exif = ExifInterface(file)
            // Add metadata
            exif.setAttribute(ExifInterface.TAG_MAKE, "3D Open Water")
            exif.setAttribute(ExifInterface.TAG_MODEL, "Camera")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "26")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, "26")
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.saveAttributes()
            //LogFileUtil.appendLog("AR screen: step 11")
            Log.d("ExifData", "EXIF data saved successfully to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("ExifData", "Failed to save EXIF data for file: ${e.message}")
            Log.e("ExifData", "Failed to save EXIF data for file: ${e.message}")
        }
    }
    private fun fixImageRotation(bitmap: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(90f) // Rotate by 90 degrees to fix the orientation
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }



    private fun calculateDistance(point1: Vector3, point2: Vector3): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        val dz = point1.z - point2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun changeBulletToGreen(bulletNode: AnchorNode) {
        bulletNode.renderable = ShapeFactory.makeSphere(0.007f, Vector3.zero(), greenMaterial).apply {
            isShadowCaster = false
            isShadowReceiver = false
        }
    }

    private fun resetBulletToGray(bulletNode: AnchorNode) {
        bulletNode.renderable = ShapeFactory.makeSphere(0.007f, Vector3.zero(), grayMaterial).apply {
            isShadowCaster = false
            isShadowReceiver = false
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
    }
}

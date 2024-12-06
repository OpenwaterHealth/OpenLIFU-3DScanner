package com.example.facedetectionar

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
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
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import java.util.EnumSet
import androidx.exifinterface.media.ExifInterface
import com.google.ar.core.Config
import com.google.ar.core.Session

class CustomArFragment : ArFragment() {

}

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var startButton: Button
    private lateinit var grayMaterial: Material
    private lateinit var greenMaterial: Material
    private val bulletNodes = mutableListOf<AnchorNode>() // Track bullet nodes
    private var closestBulletNode: AnchorNode? = null // Track closest bullet node
    private var referenceNumber: String = "DEFAULT_REF" // Default reference number
    private var imageCounter: Int = 1
    private lateinit var distanceLabel: TextView
    private var noseCoordinates: Triple<Float, Float, Float>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            LogFileUtil.appendLog("AR session screen opening")
            // Start capturing Logcat output
            LogFileUtil.appendLog("Logcat Capture starting")
            LogFileUtil.startLogcatCapture()
            LogFileUtil.appendLog("Logcat Capture started")

            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            LogFileUtil.appendLog("AR session layout set successfully")

            // Initialize UI components
            distanceLabel = findViewById(R.id.distanceLabel)
            arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
            startButton = findViewById(R.id.startButton)
            LogFileUtil.appendLog("AR Fragment and UI elements initialized")

            // Set the reference number from the previous screen
            referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "DEFAULT_REF"
            LogFileUtil.appendLog("Reference number set: $referenceNumber")

            // Retrieve nose coordinates from the intent
            noseCoordinates = intent.getSerializableExtra("NOSE_COORDINATES") as? Triple<Float, Float, Float>
            if (noseCoordinates != null) {
                LogFileUtil.appendLog("Nose Coordinates received: $noseCoordinates")
            } else {
                LogFileUtil.appendLog("No Nose Coordinates Received")
            }

            // Disable AR plane detection
            LogFileUtil.appendLog("Disabling plane detection and visual elements")
            arFragment.planeDiscoveryController.hide()
            arFragment.planeDiscoveryController.setInstructionView(null)
            arFragment.arSceneView.planeRenderer.isVisible = false

            // Configure AR session
            val session = try {
                LogFileUtil.appendLog("Initializing AR session")
                Session(this)
            } catch (e: Exception) {
                LogFileUtil.appendLog("Failed to initialize AR session: ${e.message}")
                Toast.makeText(this, "AR session initialization failed", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val config = Config(session).apply {
                focusMode = Config.FocusMode.AUTO
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            }

            try {
                LogFileUtil.appendLog("Configuring AR session")
                session.configure(config)
                arFragment.arSceneView.setupSession(session)
                LogFileUtil.appendLog("AR session configured successfully")
            } catch (e: Exception) {
                LogFileUtil.appendLog("AR session configuration failed: ${e.message}")
                Toast.makeText(this, "AR session configuration failed", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Pre-render materials and then initialize bullets
            preRenderMaterials {
                LogFileUtil.appendLog("Materials pre-rendered successfully")
                startButton.setOnClickListener {
                    LogFileUtil.appendLog("Start button clicked: initializing bullets")
                    placeDynamicBulletsAtCameraFocusFlat() // Place the ring dynamically
                    startButton.visibility = View.GONE // Hide button after placement

                    LogFileUtil.appendLog("Logcat Capture stopped")
                    // Stop capturing Logcat output
                    LogFileUtil.stopLogcatCapture()

                    startBulletTracking() // Start tracking bullets


                }
            }

            // Check for camera permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                LogFileUtil.appendLog("Camera permission not granted: requesting permissions")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            } else {
                LogFileUtil.appendLog("Camera permission already granted")
            }

            LogFileUtil.appendLog("AR screen setup completed successfully")
        } catch (e: Exception) {
            LogFileUtil.appendLog("Exception in onCreate: ${e.message}")
            e.printStackTrace()
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
            LogFileUtil.appendLog("AR screen preRenderMaterials: ${e.message}")
        }
    }

    private fun checkMaterialsReady(onComplete: () -> Unit) {
        if (::grayMaterial.isInitialized && ::greenMaterial.isInitialized) {
            onComplete()
        }
    }

    private fun placeDynamicBulletsAtCameraFocusFlat() {
        try{
            LogFileUtil.appendLog("AR screen: step 7")
            val frame: Frame = arFragment.arSceneView.arFrame ?: return
            val cameraPose: Pose = frame.camera.pose

            // Calculate position 1 meter in front of the camera
            //val forwardVector = floatArrayOf(UP/DOWN, 0f, CLOSE/FAR)
            val forwardVector = floatArrayOf(-0.14f, 0f, -0.4f)

            val cameraPosition = cameraPose.transformPoint(forwardVector)

            // Define a flat orientation (identity quaternion for no rotation)
            val flatOrientation = floatArrayOf(0f, 0f, 0f, 1f)

            val session = arFragment.arSceneView.session ?: return
            val anchor = session.createAnchor(
                Pose(cameraPosition, flatOrientation) // Flat orientation
            )

            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            val radius = 0.18f // Radius of the ring
            val bulletCount = 70

            for (i in 0 until bulletCount) {
                val angle = 2 * Math.PI * i / bulletCount - Math.PI /-2// Start from front
                val x = (radius * kotlin.math.cos(angle)).toFloat()
                val z = (radius * kotlin.math.sin(angle)).toFloat()

                val position = Vector3(x, 0f, z)
                val bulletNode = AnchorNode()
                bulletNode.setParent(anchorNode)
                bulletNode.localPosition = position

                bulletNodes.add(bulletNode) // Add bullet node to the list

                // Assign gray material initially
                bulletNode.renderable = ShapeFactory.makeSphere(0.007f, Vector3.zero(), grayMaterial).apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }

                // Add bullet ID as text
                // addBulletIdText(bulletNode, i)

                // Log bullet placement
                //Log.d("BulletPlacement", "Bullet ID: $i, Coordinates: (${position.x}, ${position.y}, ${position.z})")
            }

            // Add vertical bullets along the y-axis
            // Add vertical bullets in a rainbow shape
            val verticalRadius = 0.18f // Radius for the vertical arc
            val verticalBulletCount = 20 // Number of bullets for the vertical arc

            for (i in 0 until verticalBulletCount) {
                if(i>0 && i <(verticalBulletCount-10)) {
                    // Calculate the angle for the rainbow arc
                    val angle =
                        Math.PI * i / (verticalBulletCount - 1) // Semi-circle arc from 0 to 180 degrees
                    val y =
                        (verticalRadius * kotlin.math.sin(angle)).toFloat() // Calculate vertical position (height)
                    val z =
                        (verticalRadius * kotlin.math.cos(angle)).toFloat() // Calculate depth position (curve)

                    val position = Vector3(0f, y, z) // Bullets placed along the vertical arc
                    val bulletNode = AnchorNode()
                    bulletNode.setParent(anchorNode)
                    bulletNode.localPosition = position

                    bulletNodes.add(bulletNode) // Add bullet node to the list

                    // Assign gray material initially
                    bulletNode.renderable =
                        ShapeFactory.makeSphere(0.007f, Vector3.zero(), grayMaterial).apply {
                            isShadowCaster = false
                            isShadowReceiver = false
                        }

                    // Add bullet ID as text
                    // addBulletIdText(bulletNode, bulletCount + i) // Continue bullet IDs

                    // Log bullet placement
                    //Log.d("RainbowBulletPlacement","Rainbow Bullet ID: ${bulletCount + i}, Coordinates: (${position.x}, ${position.y}, ${position.z})")
                }
            }


            //Toast.makeText(this, "Ring placed flat at camera focus!", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception)
        {
            LogFileUtil.appendLog("AR screen placeDynamicBulletsAtCameraFocusFlat: ${e.message}")
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
        try{
            LogFileUtil.appendLog("AR screen: step 8")
            arFragment.arSceneView.scene.addOnUpdateListener {
                val frame: Frame = arFragment.arSceneView.arFrame ?: return@addOnUpdateListener
                val cameraPose = frame.camera.pose
                val cameraPosition = Vector3(cameraPose.tx(), cameraPose.ty(), cameraPose.tz())

                var closestDistance = Float.MAX_VALUE
                var closestNode: AnchorNode? = null

                bulletNodes.forEach { bulletNode ->
                    val bulletPosition = bulletNode.worldPosition // Get global position of the bullet
                    val distance = calculateDistance(cameraPosition, bulletPosition)

                    // Log distances for debugging
                    //Log.d("BulletTracking", "Bullet Position: ${bulletPosition}, Distance: $distance")

                    if (distance < closestDistance) { // Find the closest bullet
                        closestDistance = distance
                        closestNode = bulletNode

                        // Update the distance label based on the distance
                        when {
                            distance < 0.2f -> {
                                distanceLabel.text = "Too Close"
                                distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark)) // Set text color to red
                            }
                            distance > 0.4f -> {
                                distanceLabel.text = "Too Far"
                                distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark)) // Set text color to red
                            }
                            else -> {
                                distanceLabel.text = "Good"
                                distanceLabel.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark)) // Set text color to green
                            }
                        }

                    }
                }

                // Update bullet colors
                updateBulletColors(closestNode)
            }
        }
        catch (e: Exception)
        {
            LogFileUtil.appendLog("AR screen startBulletTracking: ${e.message}")
        }
    }
    val greenBulletList = mutableSetOf<AnchorNode>() // Ensures no duplicate entries

    private fun updateBulletColors(closestNode: AnchorNode?) {
        // Initialize a list to track bullets that are green

        if (closestNode != null && !greenBulletList.contains(closestNode)) { // Only update if the bullet is not already green
            // Turn the closest bullet green
            changeBulletToGreen(closestNode)

            // Add the newly green bullet to the list
            greenBulletList.add(closestNode)


            // Trigger vibration
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



            // Capture a photo when a bullet turns green
            captureCameraFeedPhoto(arFragment.arSceneView)

            // Check if all bullets are green
            if (greenBulletList.size == bulletNodes.size) {
                navigateToUsbScreen()
            }

            Log.d("GreenBulletTracking", "Bullet at position ${closestNode.worldPosition} added to greenBulletList.")
        }

        closestBulletNode = closestNode // Update the tracked closest bullet
    }

    private fun navigateToUsbScreen() {
        LogFileUtil.appendLog("Transferring to Image transfer screen, connect usb")
        val intent = Intent(this, UsbScreenActivity::class.java)
        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
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

            LogFileUtil.appendLog("AR screen: step 10: Photo saved")
            Log.d("CapturePhoto", "Photo saved at: $savedUri")
            //Toast.makeText(arSceneView.context, "Photo saved at: $savedUri", Toast.LENGTH_SHORT).show()

            image.close() // Release the image
        } catch (e: Exception) {
            LogFileUtil.appendLog("AR screen: Failed to capture photo: ${e.message}")
            Log.e("CapturePhoto", "Failed to capture photo: ${e.message}")
        }
    }

    private fun convertImageToBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val outStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, outStream)

        val jpegBytes = outStream.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun saveBitmapToStorage(bitmap: Bitmap) {
        try{
            val fixedBitmap = fixImageRotation(bitmap) // Rotate the image if needed
            val filename = "${referenceNumber}_${imageCounter}.jpeg"
            imageCounter++ // Increment the counter for the next image

            val savedUri: Uri?
            val savedFilePath: String?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                }

                savedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                val fos = savedUri?.let { resolver.openOutputStream(it) }

                fos?.use {
                    fixedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
                savedFilePath = savedUri.toString()

                // Add EXIF data using InputStream/OutputStream
                savedUri?.let {
                    addExifDataForUri(it)
                }
            } else {
                val imagesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "3DOpenWater")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                val savedFile = File(imagesDir, filename)
                FileOutputStream(savedFile).use { fos ->
                    fixedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                savedFilePath = savedFile.absolutePath

                // Add EXIF data directly to the file
                addExifDataForFile(savedFile)
            }

            Log.d("SaveImage", "Image saved as $filename at $savedFilePath")
            //Toast.makeText(this, "Image saved as $filename", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception)
        {
            LogFileUtil.appendLog("AR screen saveBitmapToStorage: ${e.message}")
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
            LogFileUtil.appendLog("AR screen: step 11, Failed to save EXIF data for URI: ${e.message}")
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
            LogFileUtil.appendLog("AR screen: step 11")
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

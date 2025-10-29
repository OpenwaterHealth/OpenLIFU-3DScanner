package com.example.facedetectionar

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.facedetectionar.api.repository.CloudRepository
import com.example.facedetectionar.api.repository.UserRepository
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class New_capture : AppCompatActivity() {
    @Inject
    lateinit var cloudRepository: CloudRepository
    @Inject
    lateinit var userRepository: UserRepository

    private val REQUIRED_PERMISSIONS = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )

        else -> arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var newCaptureCheckbox: CheckBox
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_new_capture)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Check required permissions
        if (!hasAllPermissions()) {
            requestPermissions()
        }

        // Check for All Files Access permission on Android 11+
        checkAllFilesAccessPermission()

        // Check and ensure ARCore availability
        checkAndInstallARCore()




        initializeRingConfig()


//        val linearLayout=findViewById<LinearLayout>(R.id.linerLayoutOfParameters)
        val errorText = findViewById<TextView>(R.id.errorText)
        val cancelButton = findViewById<Button>(R.id.cancelButton);
//        val qrIconButton=findViewById<ImageButton>(R.id.qrIconButton);
        val startCaptureButton = findViewById<Button>(R.id.startCaptureButton);
        newCaptureCheckbox = findViewById<CheckBox>(R.id.newCaptureCheckbox)

        val autoUploadCheckbox = findViewById<CheckBox>(R.id.autoUploadCheckbox)
        val autoUploadContainer = findViewById<View>(R.id.autoUploadContainer)

        val referenceNumberEditText = findViewById<EditText>(R.id.scanIDInputText)

        val fetchedQRText = intent.getStringExtra("QR_TEXT")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val doNotShowInfoPref = prefs.getBoolean("do_not_show_info", false)


        if (!userRepository.authService.isSignedIn()) {
            autoUploadCheckbox.isChecked = false
            autoUploadContainer.visibility = View.GONE
        }

        if (doNotShowInfoPref) {
            newCaptureCheckbox.isChecked = false;
        }









        Log.d("IDDDDD", "Launched ${fetchedQRText}")
        if (!fetchedQRText.isNullOrEmpty()) {
            referenceNumberEditText.setText(fetchedQRText)

        }

        referenceNumberEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    errorText.visibility = View.INVISIBLE
                    referenceNumberEditText.setBackgroundResource(R.drawable.edit_text_border_green)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })



        referenceNumberEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // User is typing, show green border
                referenceNumberEditText.setBackgroundResource(R.drawable.edit_text_border_green)
            } else {
                // Lost focus
                if (referenceNumberEditText.text.isNullOrBlank()) {

                    referenceNumberEditText.setBackgroundResource(R.drawable.input_border_red)
                } else {
                    // Input entered -> Normal background
                    referenceNumberEditText.setBackgroundResource(R.drawable.rounded_edittext_background)
                }
            }
        }


        val dialog = Dialog(this);












        cancelButton.setOnClickListener {
            val intent = Intent(this, welcomeActivity::class.java)
            startActivity(intent);

        }

//        qrIconButton.setOnClickListener {
//            Log.d("QR activity","Launched")
//            val intent = Intent(this, QrActivity::class.java)
//            startActivity(intent)
//        }


        // Handle submit button click
        startCaptureButton.setOnClickListener {
            val referenceNumber = referenceNumberEditText.text.toString().trim()

            if (referenceNumber.isEmpty()) {

                errorText.visibility = View.VISIBLE
                referenceNumberEditText.setBackgroundResource(R.drawable.input_border_red)
            } else if (hasAllPermissions()) {
                //LogFileUtil.appendLog("Moving to face detection screen")

                cloudRepository.createPhotocollection(referenceNumber, autoUploadCheckbox.isChecked)
                navigateToFaceDetection(referenceNumber)
            } else {
                showToastAndLog("Permissions are required to proceed")
                requestPermissions()
            }
        }


    }


    // Check if all permissions are granted
    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request the necessary permissions
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    // Navigate to the FaceDetectionActivity
    private fun navigateToFaceDetection(referenceNumber: String) {

        val view = layoutInflater.inflate(R.layout.instructions, null)
        val dialog = Dialog(this)
        val okInInstructions = view.findViewById<Button>(R.id.instructionModalOK)

        val checkInfo = view.findViewById<CheckBox>(R.id.checkBoxInfo)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Handle OK button click
        okInInstructions.setOnClickListener {

            if (checkInfo.isChecked) {
                prefs.edit().putBoolean("do_not_show_info", true).apply()
                Log.d("Preferene", "dont show modal now")

            } else {
                prefs.edit().putBoolean("do_not_show_info", false).apply()
                Log.d("Preferene", "Will show modal now")
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("REFERENCE_NUMBER", referenceNumber)
            startActivity(intent)
            finish()


        }


        if (newCaptureCheckbox.isChecked) {

            // Set the inflated view as content


            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val marginInPx = (20 * metrics.density).toInt()
            val dialogWidth = screenWidth - (marginInPx * 2)

            dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(false)

            // Initialize VideoView from the inflated view
            val instructionVideo = view.findViewById<VideoView>(R.id.instructionVideo)

            val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.instruction_video}")

            instructionVideo.setVideoURI(videoUri)
            instructionVideo.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start() // Start playback when prepared
            }



            dialog.show()

        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("REFERENCE_NUMBER", referenceNumber)
            startActivity(intent)
            finish()
        }

    }

    // Show a toast and log a message
    private fun showToastAndLog(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        //LogFileUtil.appendLog(message)
    }

    // Check for All Files Access permission on Android 11+
    private fun checkAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showToastAndLog("Requesting All Files Access permission")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${applicationContext.packageName}")
                startActivity(intent)
            }
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showToastAndLog("Permissions granted")
                initializeRingConfig()
            } else {
                showToastAndLog("Permissions are required to proceed")
            }
        }
    }

    private fun checkAndInstallARCore() {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)

            when {
                availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    Log.d("ARCoreCheck", "ARCore is supported but not installed. Prompting user.")
                    showARCoreInstallPrompt() // Show the install prompt
                }

                availability.isTransient -> {
                    // Retry if status is transient
                    Log.d("ARCoreCheck", "ARCore availability is transient. Retrying in 2 seconds.")
                    Handler(Looper.getMainLooper()).postDelayed({ checkAndInstallARCore() }, 2000)
                }

                availability.isSupported -> {
                    Log.d("ARCoreCheck", "ARCore is supported and installed.")
                    return // ARCore is supported and ready to use
                }

                else -> {
                    Log.d("ARCoreCheck", "ARCore is not supported on this device.")
                    Toast.makeText(
                        this,
                        "Your device does not support AR features.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e("ARCoreCheck", "This device is not compatible with ARCore: ${e.message}")
            Toast.makeText(this, "Your device does not support AR features.", Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            Log.e("ARCoreCheck", "Error while checking ARCore availability: ${e.message}")
        }
    }

    private fun initializeRingConfig() {
        makeRingConfigJsonFile()


    }


    private fun showARCoreInstallPrompt() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Install Required")
        dialogBuilder.setMessage("You need to install 'Google Play Services for AR' to use this application.")
        dialogBuilder.setPositiveButton("Install Now") { _, _ ->
            redirectToARCorePlayStore()
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss() // Close the dialog
        }
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun redirectToARCorePlayStore() {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.core")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ARCoreCheck", "Failed to open Play Store: ${e.message}")
            Toast.makeText(
                this,
                "Please install Google Play Services for AR manually.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // Creates the by default config file for the ring placement
    private fun makeRingConfigJsonFile() {
        Log.d("makeRingConfigJsonFile", "makeRingConfigJsonFile called")

        try {
            val filename = "ringConfig.json"
            val rootDir = File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/")
            if (!rootDir.exists()) rootDir.mkdirs()

            val savedFile = File(rootDir, filename)

            //Check if file already exists → don't recreate
            if (savedFile.exists()) {
                Log.d("makeRingConfigJsonFile", "File already exists. Skipping creation.")
                return
            }


            val arrowCoordinatesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("seqID", 0)
                    put("xPoint", -0.006312673)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.15819661)
                    put("verticalAngle", 30)
                    put("horizontalAngle", 0)
                })
                put(JSONObject().apply {
                    put("seqID", 1)
                    put("xPoint", -0.06811607)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.14840792)
                    put("verticalAngle", 26)
                    put("horizontalAngle", 18)
                })
                put(JSONObject().apply {
                    put("seqID", 2)
                    put("xPoint", -0.12386973)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.12000000)
                    put("verticalAngle", 27)
                    put("horizontalAngle", 36)
                })
                put(JSONObject().apply {
                    put("seqID", 3)
                    put("xPoint", -0.16811607)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.07575366)
                    put("verticalAngle", 24)
                    put("horizontalAngle", 54)
                })
                put(JSONObject().apply {
                    put("seqID", 4)
                    put("xPoint", -0.19652398)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.02000001)
                    put("verticalAngle",28)
                    put("horizontalAngle", 72)
                })
                put(JSONObject().apply {
                    put("seqID", 5)
                    put("xPoint", -0.20631267)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.04180339)
                    put("verticalAngle", 34)
                    put("horizontalAngle", 90)
                })
                put(JSONObject().apply {
                    put("seqID", 6)
                    put("xPoint", -0.19652398)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.10360679)
                    put("verticalAngle", 27)
                    put("horizontalAngle", 108)
                })
                put(JSONObject().apply {
                    put("seqID", 7)
                    put("xPoint", -0.16811607)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.15936044)
                    put("verticalAngle", 39)
                    put("horizontalAngle", 126)
                })
                put(JSONObject().apply {
                    put("seqID", 8)
                    put("xPoint", -0.12386973)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.20360684)
                    put("verticalAngle", 40)
                    put("horizontalAngle", 144)
                })
                put(JSONObject().apply {
                    put("seqID", 9)
                    put("xPoint", -0.06811607)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.23201469)
                    put("verticalAngle", 34)
                    put("horizontalAngle", 162)
                })
                put(JSONObject().apply {
                    put("seqID", 10)
                    put("xPoint", -0.006312673)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.24180338)
                    put("verticalAngle", 10)
                    put("horizontalAngle", 180)
                })
                put(JSONObject().apply {
                    put("seqID", 11)
                    put("xPoint", 0.0554907277)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.23201469)
                    put("verticalAngle", 30)
                    put("horizontalAngle", 198)
                })
                put(JSONObject().apply {
                    put("seqID", 12)
                    put("xPoint", 0.111244376)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.20360684)
                    put("verticalAngle", 40)
                    put("horizontalAngle", 216)
                })
                put(JSONObject().apply {
                    put("seqID", 13)
                    put("xPoint", 0.15549072)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.15936044)
                    put("verticalAngle", 41)
                    put("horizontalAngle", 0)
                })
                put(JSONObject().apply {
                    put("seqID", 14)
                    put("xPoint", 0.18389865)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.10360679)
                    put("verticalAngle", 20)
                    put("horizontalAngle", 234)
                })
                put(JSONObject().apply {
                    put("seqID", 15)
                    put("xPoint", 0.19368734)
                    put("yPoint", 0.030656703)
                    put("zPoint", -0.04180339)
                    put("verticalAngle", 14)
                    put("horizontalAngle", 252)
                })
                put(JSONObject().apply {
                    put("seqID", 16)
                    put("xPoint", 0.18389865)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.02000001)
                    put("verticalAngle", 27)
                    put("horizontalAngle", 270)
                })
                put(JSONObject().apply {
                    put("seqID", 17)
                    put("xPoint", 0.15549072)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.07575366)
                    put("verticalAngle", 11)
                    put("horizontalAngle", 288)
                })
                put(JSONObject().apply {
                    put("seqID", 18)
                    put("xPoint", 0.111244376)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.12000000)
                    put("verticalAngle", 12)
                    put("horizontalAngle", 306)
                })
                put(JSONObject().apply {
                    put("seqID", 19)
                    put("xPoint", 0.0554907277)
                    put("yPoint", 0.030656703)
                    put("zPoint", 0.14840792)
                    put("verticalAngle", 10)
                    put("horizontalAngle", 324)
                })

                put(JSONObject().apply {
                    put("seqID", 20)
                    put("xPoint", 0.0)
                    put("yPoint", 0.0)
                    put("zPoint", 0.0)
                    put("verticalAngle", 90)
                    put("horizontalAngle", 0)
                })

            }







            // Wrap it in a root JSON object
            val rootObject = JSONObject().apply {
                put("bulletCoordinates", arrowCoordinatesArray);
                // Add cameraDistanceConfig object
                put("cameraDistanceForCapture", JSONObject().apply {
                    put("maxDistance", 0.34)
                    put("minDistance", 0.32)
                })

                // Add cameraDistanceConfig object
                put("cameraDistanceForFaceDetection", JSONObject().apply {
                    put("maxDistance",-9.0)
                    put("minDistance", -10.0)
                })


                // Add cube configurations
                put("cubeConfig", JSONObject().apply {


                    put("cubeSize",0.35)
                })


                put("ringAroundFaceConfig", JSONObject().apply {
                    put("initialRingSize",0.05)
                    put("ringScaleFactor",3.2)

                })

                put("ringWithCamera", JSONObject().apply {
                    put("ringSize",250)
                })

                put("cameraCaptureDelayAndSpeed", JSONObject().apply {
                    put("maxAllowedSpeed",0.6)
                    put("captureDelayTime",1000)
                })


            }



            // Write to file (pretty print)
            savedFile.writeText(rootObject.toString(4))

            Log.d("makeRingConfigJsonFile", "ringConfig.json created at: ${savedFile.absolutePath}")


        } catch (e: Exception) {
            Log.e("makeRingConfigJsonFile", "Error creating JSON file: ${e.message}")
        }
    }


}
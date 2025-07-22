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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetectionar.Adapters.ArcConfigAdapter
import com.example.facedetectionar.Modals.ArcConfig
import com.example.facedetectionar.Modals.RingParameter
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import org.json.JSONArray
import org.json.JSONObject


class New_capture : AppCompatActivity() {
    val ringPrametersList=mutableListOf<ArcConfig>()
    private lateinit var dotsContainer: LinearLayout
    private lateinit var dots: ArrayList<ImageView>
    private var showDynamicPoints: Boolean=false;

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
    private lateinit var   newCaptureCheckbox: CheckBox
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


        //make json config file
//        makeRingConfigJsonFile()



//        loadsRingConfigFromJson()

        initializeRingConfig()






//recycler setup
//        val recyclerView = findViewById<RecyclerView>(R.id.parametersRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        recyclerView.adapter = ArcConfigAdapter(ringPrametersList)
//          // Snap helper to snap items like pages
//        val snapHelper = LinearSnapHelper()
//        snapHelper.attachToRecyclerView(recyclerView)
//
//
//        setupDotsIndicator()
//        setupRecyclerViewWithDots()









//        val linearLayout=findViewById<LinearLayout>(R.id.linerLayoutOfParameters)
        val errorText=findViewById<TextView>(R.id.errorText)
        val cancelButton=findViewById<Button>(R.id.cancelButton);
//        val qrIconButton=findViewById<ImageButton>(R.id.qrIconButton);
        val startCaptureButton=findViewById<Button>(R.id.startCaptureButton);
        newCaptureCheckbox=findViewById<CheckBox>(R.id.newCaptureCheckbox)
        val referenceNumberEditText = findViewById<EditText>(R.id.scanIDInputText)

        val fetchedQRText=intent.getStringExtra("QR_TEXT")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val doNotShowInfoPref = prefs.getBoolean("do_not_show_info", false)
        val recyclerContainer=findViewById<LinearLayout>(R.id.recyclerContainer)

        if(showDynamicPoints){
            recyclerContainer.visibility=View.GONE;
        }

        if(doNotShowInfoPref){
            newCaptureCheckbox.isChecked=false;
        }









        Log.d("IDDDDD","Launched ${fetchedQRText}")
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





        val dialog= Dialog(this);












        cancelButton.setOnClickListener {
            val intent= Intent(this, welcomeActivity::class.java)
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

                errorText.visibility=View.VISIBLE
                referenceNumberEditText.setBackgroundResource(R.drawable.input_border_red)
            } else if (hasAllPermissions()) {
                //LogFileUtil.appendLog("Moving to face detection screen")

                navigateToFaceDetection(referenceNumber)
            } else {
                showToastAndLog("Permissions are required to proceed")
                requestPermissions()
            }
        }





    }
//    here on create ends

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

        val view=layoutInflater.inflate(R.layout.instructions,null)
        val dialog= Dialog(this)
        val okInInstructions=view.findViewById<Button>(R.id.instructionModalOK)

        val checkInfo=view.findViewById<CheckBox>(R.id.checkBoxInfo)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Handle OK button click
        okInInstructions.setOnClickListener {

            if (checkInfo.isChecked) {
                prefs.edit().putBoolean("do_not_show_info", true).apply()
                Log.d("Preferene","dont show modal now")

            } else {
                prefs.edit().putBoolean("do_not_show_info", false).apply()
                Log.d("Preferene","Will show modal now")
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("REFERENCE_NUMBER", referenceNumber)
            startActivity(intent)
            finish()


        }


        if(newCaptureCheckbox.isChecked){

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

        }else{
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
                    Toast.makeText(this, "Your device does not support AR features.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e("ARCoreCheck", "This device is not compatible with ARCore: ${e.message}")
            Toast.makeText(this, "Your device does not support AR features.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("ARCoreCheck", "Error while checking ARCore availability: ${e.message}")
        }
    }

    private fun initializeRingConfig() {
        // 1. Make sure config file exists
        makeRingConfigJsonFile()

        // 2. Load config from file
        loadsRingConfigFromJson()

        checkShowCoordinates()

        // 3. Setup UI with the loaded config
        setupRingConfigUI()
    }

    private fun setupRingConfigUI() {
        val recyclerView = findViewById<RecyclerView>(R.id.parametersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = ArcConfigAdapter(ringPrametersList)

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        setupDotsIndicator()
        setupRecyclerViewWithDots()
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.core"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ARCoreCheck", "Failed to open Play Store: ${e.message}")
            Toast.makeText(this, "Please install Google Play Services for AR manually.", Toast.LENGTH_LONG).show()
        }
    }


    // ring config json
    private fun makeRingConfigJsonFile() {
        Log.d("makeRingConfigJsonFile", "makeRingConfigJsonFile called")

        try {
            val filename = "ringConfig.json"
            val rootDir = File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/")
            if (!rootDir.exists()) rootDir.mkdirs()

            val savedFile = File(rootDir, filename)

            // ✅ Check if file already exists → don't recreate
            if (savedFile.exists()) {
                Log.d("makeRingConfigJsonFile", "File already exists. Skipping creation.")
                return
            }

            // Create arcs array
            val arcsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "horizontal")
                    put("radius", 0.20)
                    put("bulletCount", 30)
                    put("upDown", -0.05)
                    put("closeFar", -0.43)
                    put("minAngle", 0)
                    put("maxAngle", 30)


                })
                put(JSONObject().apply {
                    put("type", "horizontal")
                    put("radius", 0.20)
                    put("bulletCount", 20)
                    put("upDown", -0.12)
                    put("closeFar", -0.43)
                    put("minAngle", 0)
                    put("maxAngle", 40)
                })

                put(JSONObject().apply {
                    put("type", "horizontal")
                    put("radius", 0.20)
                    put("bulletCount", 30)
                    put("upDown", 0.00)
                    put("closeFar", -0.43)
                    put("minAngle", 0)
                    put("maxAngle", 30)
                })
            }


            //bullets co-oridinates array
            val bulletCoordinatesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 0)
                    put("xPoint", -0.010153387)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.22783317)
                    put("minAngle", 18)
                    put("maxAngle", 78)
                })
                put(JSONObject().apply {
                    put("id", 1)
                    put("xPoint", -0.07195678)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.23762186)
                    put("minAngle", 15)
                    put("maxAngle", 32)
                })
                put(JSONObject().apply {
                    put("id", 2)
                    put("xPoint", -0.12771043)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.26602978)
                    put("minAngle", 18)
                    put("maxAngle", 54)
                })
                put(JSONObject().apply {
                    put("id", 3)
                    put("xPoint", -0.17195678)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.31027612)
                    put("minAngle", 18)
                    put("maxAngle", 32)
                })
                put(JSONObject().apply {
                    put("id", 4)
                    put("xPoint", -0.2003647)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.36602977)
                    put("minAngle", 22)
                    put("maxAngle", 107)
                })
                put(JSONObject().apply {
                    put("id", 5)
                    put("xPoint", -0.21015339)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.42783317)
                    put("minAngle", 21)
                    put("maxAngle", 79)
                })
                put(JSONObject().apply {
                    put("id", 6)
                    put("xPoint", -0.2003647)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.48963657)
                    put("minAngle", 23)
                    put("maxAngle", 47)
                })
                put(JSONObject().apply {
                    put("id", 7)
                    put("xPoint", -0.17195678)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.54539025)
                    put("minAngle", 24)
                    put("maxAngle", 108)
                })
                put(JSONObject().apply {
                    put("id", 8)
                    put("xPoint", -0.12771043)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.58963656)
                    put("minAngle", 18)
                    put("maxAngle", 81)
                })
                put(JSONObject().apply {
                    put("id", 9)
                    put("xPoint", -0.07195678)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.6180445)
                    put("minAngle", 19)
                    put("maxAngle", 38)
                })
                put(JSONObject().apply {
                    put("id", 10)
                    put("xPoint", -0.010153387)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.6278332)
                    put("minAngle", 19)
                    put("maxAngle", 55)
                })
                put(JSONObject().apply {
                    put("id", 11)
                    put("xPoint", 0.051650014)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.6180445)
                    put("minAngle", 23)
                    put("maxAngle", 108)
                })
                put(JSONObject().apply {
                    put("id", 12)
                    put("xPoint", 0.107403666)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.58963656)
                    put("minAngle", 21)
                    put("maxAngle", 63)
                })
                put(JSONObject().apply {
                    put("id", 13)
                    put("xPoint", 0.15165001)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.54539025)
                    put("minAngle", 21)
                    put("maxAngle", 99)
                })
                put(JSONObject().apply {
                    put("id", 14)
                    put("xPoint", 0.18005793)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.48963657)
                    put("minAngle", 21)
                    put("maxAngle", 42)
                })
                put(JSONObject().apply {
                    put("id", 15)
                    put("xPoint", 0.18984662)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.42783317)
                    put("minAngle", 22)
                    put("maxAngle", 58)
                })
                put(JSONObject().apply {
                    put("id", 16)
                    put("xPoint", 0.18005793)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.36602977)
                    put("minAngle", 18)
                    put("maxAngle", 87)
                })
                put(JSONObject().apply {
                    put("id", 17)
                    put("xPoint", 0.15165001)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.31027612)
                    put("minAngle", 15)
                    put("maxAngle", 98)
                })
                put(JSONObject().apply {
                    put("id", 18)
                    put("xPoint", 0.107403666)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.26602978)
                    put("minAngle", 21)
                    put("maxAngle", 58)
                })
                put(JSONObject().apply {
                    put("id", 19)
                    put("xPoint", 0.051650014)
                    put("yPoint", 0.10050259)
                    put("zPoint", -0.23762186)
                    put("minAngle", 15)
                    put("maxAngle", 45)
                })
                put(JSONObject().apply {
                    put("id", 20)
                    put("xPoint", -0.010322976)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.22299527)
                    put("minAngle", 22)
                    put("maxAngle", 31)
                })
                put(JSONObject().apply {
                    put("id", 21)
                    put("xPoint", -0.051905315)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.22736575)
                    put("minAngle", 25)
                    put("maxAngle", 119)
                })
                put(JSONObject().apply {
                    put("id", 22)
                    put("xPoint", -0.091670305)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.24028617)
                    put("minAngle", 15)
                    put("maxAngle", 91)
                })
                put(JSONObject().apply {
                    put("id", 23)
                    put("xPoint", -0.12788002)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.26119187)
                    put("minAngle", 19)
                    put("maxAngle", 67)
                })
                put(JSONObject().apply {
                    put("id", 24)
                    put("xPoint", -0.15895194)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.28916913)
                    put("minAngle", 25)
                    put("maxAngle", 91)
                })
                put(JSONObject().apply {
                    put("id", 25)
                    put("xPoint", -0.18352805)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.32299528)
                    put("minAngle", 18)
                    put("maxAngle", 110)
                })
                put(JSONObject().apply {
                    put("id", 26)
                    put("xPoint", -0.20053428)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.36119187)
                    put("minAngle", 23)
                    put("maxAngle", 120)
                })
                put(JSONObject().apply {
                    put("id", 27)
                    put("xPoint", -0.20922735)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.40208957)
                    put("minAngle", 20)
                    put("maxAngle", 75)
                })
                put(JSONObject().apply {
                    put("id", 28)
                    put("xPoint", -0.20922735)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.44390097)
                    put("minAngle", 16)
                    put("maxAngle", 105)
                })
                put(JSONObject().apply {
                    put("id", 29)
                    put("xPoint", -0.20053428)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.48479867)
                    put("minAngle", 25)
                    put("maxAngle", 68)
                })
                put(JSONObject().apply {
                    put("id", 30)
                    put("xPoint", -0.18352805)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.5229953)
                    put("minAngle", 20)
                    put("maxAngle", 54)
                })
                put(JSONObject().apply {
                    put("id", 31)
                    put("xPoint", -0.15895194)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.5568214)
                    put("minAngle", 21)
                    put("maxAngle", 109)
                })
                put(JSONObject().apply {
                    put("id", 32)
                    put("xPoint", -0.12788002)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.5847987)
                    put("minAngle", 19)
                    put("maxAngle", 89)
                })
                put(JSONObject().apply {
                    put("id", 33)
                    put("xPoint", -0.091670305)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.60570437)
                    put("minAngle", 15)
                    put("maxAngle", 118)
                })
                put(JSONObject().apply {
                    put("id", 34)
                    put("xPoint", -0.051905315)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.6186248)
                    put("minAngle", 16)
                    put("maxAngle", 39)
                })
                put(JSONObject().apply {
                    put("id", 35)
                    put("xPoint", -0.010322976)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.62299526)
                    put("minAngle", 17)
                    put("maxAngle", 74)
                })
                put(JSONObject().apply {
                    put("id", 36)
                    put("xPoint", 0.03125936)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.6186248)
                    put("minAngle", 25)
                    put("maxAngle", 98)
                })
                put(JSONObject().apply {
                    put("id", 37)
                    put("xPoint", 0.07102436)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.60570437)
                    put("minAngle", 17)
                    put("maxAngle", 64)
                })
                put(JSONObject().apply {
                    put("id", 38)
                    put("xPoint", 0.107234076)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.5847987)
                    put("minAngle", 18)
                    put("maxAngle", 89)
                })
                put(JSONObject().apply {
                    put("id", 39)
                    put("xPoint", 0.13830599)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.5568214)
                    put("minAngle", 22)
                    put("maxAngle", 67)
                })
                put(JSONObject().apply {
                    put("id", 40)
                    put("xPoint", 0.1628821)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.5229953)
                    put("minAngle", 15)
                    put("maxAngle", 39)
                })
                put(JSONObject().apply {
                    put("id", 41)
                    put("xPoint", 0.17988834)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.48479867)
                    put("minAngle", 17)
                    put("maxAngle", 76)
                })
                put(JSONObject().apply {
                    put("id", 42)
                    put("xPoint", 0.1885814)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.44390097)
                    put("minAngle", 15)
                    put("maxAngle", 71)
                })
                put(JSONObject().apply {
                    put("id", 43)
                    put("xPoint", 0.1885814)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.40208957)
                    put("minAngle", 16)
                    put("maxAngle", 92)
                })
                put(JSONObject().apply {
                    put("id", 44)
                    put("xPoint", 0.17988834)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.36119187)
                    put("minAngle", 21)
                    put("maxAngle", 84)
                })
                put(JSONObject().apply {
                    put("id", 45)
                    put("xPoint", 0.1628821)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.32299528)
                    put("minAngle", 24)
                    put("maxAngle", 119)
                })
                put(JSONObject().apply {
                    put("id", 46)
                    put("xPoint", 0.13830599)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.28916913)
                    put("minAngle", 25)
                    put("maxAngle", 66)
                })
                put(JSONObject().apply {
                    put("id", 47)
                    put("xPoint", 0.107234076)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.26119187)
                    put("minAngle", 15)
                    put("maxAngle", 93)
                })
                put(JSONObject().apply {
                    put("id", 48)
                    put("xPoint", 0.07102436)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.24028617)
                    put("minAngle", 15)
                    put("maxAngle", 51)
                })
                put(JSONObject().apply {
                    put("id", 49)
                    put("xPoint", 0.03125936)
                    put("yPoint", 0.030670168)
                    put("zPoint", -0.22736575)
                    put("minAngle", 18)
                    put("maxAngle", 44)
                })
                put(JSONObject().apply {
                    put("id", 50)
                    put("xPoint", -0.010444114)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.21953963)
                    put("minAngle", 21)
                    put("maxAngle", 59)
                })
                put(JSONObject().apply {
                    put("id", 51)
                    put("xPoint", -0.05202645)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.22391011)
                    put("minAngle", 18)
                    put("maxAngle", 62)
                })
                put(JSONObject().apply {
                    put("id", 52)
                    put("xPoint", -0.09179144)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.23683053)
                    put("minAngle", 21)
                    put("maxAngle", 94)
                })
                put(JSONObject().apply {
                    put("id", 53)
                    put("xPoint", -0.12800117)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.25773624)
                    put("minAngle", 21)
                    put("maxAngle", 39)
                })
                put(JSONObject().apply {
                    put("id", 54)
                    put("xPoint", -0.15907308)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.2857135)
                    put("minAngle", 23)
                    put("maxAngle", 31)
                })
                put(JSONObject().apply {
                    put("id", 55)
                    put("xPoint", -0.1836492)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.31953964)
                    put("minAngle", 17)
                    put("maxAngle", 44)
                })
                put(JSONObject().apply {
                    put("id", 56)
                    put("xPoint", -0.20065543)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.35773623)
                    put("minAngle", 24)
                    put("maxAngle", 116)
                })
                put(JSONObject().apply {
                    put("id", 57)
                    put("xPoint", -0.2093485)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.39863393)
                    put("minAngle", 24)
                    put("maxAngle", 118)
                })
                put(JSONObject().apply {
                    put("id", 58)
                    put("xPoint", -0.2093485)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.44044533)
                    put("minAngle", 19)
                    put("maxAngle", 55)
                })
                put(JSONObject().apply {
                    put("id", 59)
                    put("xPoint", -0.20065543)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.48134303)
                    put("minAngle", 17)
                    put("maxAngle", 110)
                })
                put(JSONObject().apply {
                    put("id", 60)
                    put("xPoint", -0.1836492)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.51953965)
                    put("minAngle", 18)
                    put("maxAngle", 47)
                })
                put(JSONObject().apply {
                    put("id", 61)
                    put("xPoint", -0.15907308)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.55336577)
                    put("minAngle", 23)
                    put("maxAngle", 113)
                })
                put(JSONObject().apply {
                    put("id", 62)
                    put("xPoint", -0.12800117)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.58134305)
                    put("minAngle", 18)
                    put("maxAngle", 103)
                })
                put(JSONObject().apply {
                    put("id", 63)
                    put("xPoint", -0.09179144)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.6022487)
                    put("minAngle", 19)
                    put("maxAngle", 92)
                })
                put(JSONObject().apply {
                    put("id", 64)
                    put("xPoint", -0.05202645)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.61516917)
                    put("minAngle", 16)
                    put("maxAngle", 106)
                })
                put(JSONObject().apply {
                    put("id", 65)
                    put("xPoint", -0.010444114)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.6195396)
                    put("minAngle", 15)
                    put("maxAngle", 116)
                })
                put(JSONObject().apply {
                    put("id", 66)
                    put("xPoint", 0.031138225)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.61516917)
                    put("minAngle", 16)
                    put("maxAngle", 95)
                })
                put(JSONObject().apply {
                    put("id", 67)
                    put("xPoint", 0.07090322)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.6022487)
                    put("minAngle", 16)
                    put("maxAngle", 78)
                })
                put(JSONObject().apply {
                    put("id", 68)
                    put("xPoint", 0.10711294)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.58134305)
                    put("minAngle", 18)
                    put("maxAngle", 97)
                })
                put(JSONObject().apply {
                    put("id", 69)
                    put("xPoint", 0.13818485)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.55336577)
                    put("minAngle", 25)
                    put("maxAngle", 86)
                })
                put(JSONObject().apply {
                    put("id", 70)
                    put("xPoint", 0.16276096)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.51953965)
                    put("minAngle", 17)
                    put("maxAngle", 119)
                })
                put(JSONObject().apply {
                    put("id", 71)
                    put("xPoint", 0.17976719)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.48134303)
                    put("minAngle", 24)
                    put("maxAngle", 53)
                })
                put(JSONObject().apply {
                    put("id", 72)
                    put("xPoint", 0.18846026)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.44044533)
                    put("minAngle", 16)
                    put("maxAngle", 61)
                })
                put(JSONObject().apply {
                    put("id", 73)
                    put("xPoint", 0.18846026)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.39863393)
                    put("minAngle", 19)
                    put("maxAngle", 90)
                })
                put(JSONObject().apply {
                    put("id", 74)
                    put("xPoint", 0.17976719)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.35773623)
                    put("minAngle", 22)
                    put("maxAngle", 107)
                })
                put(JSONObject().apply {
                    put("id", 75)
                    put("xPoint", 0.16276096)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.31953964)
                    put("minAngle", 15)
                    put("maxAngle", 59)
                })
                put(JSONObject().apply {
                    put("id", 76)
                    put("xPoint", 0.13818485)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.2857135)
                    put("minAngle", 17)
                    put("maxAngle", 77)
                })
                put(JSONObject().apply {
                    put("id", 77)
                    put("xPoint", 0.10711294)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.25773624)
                    put("minAngle", 18)
                    put("maxAngle", 59)
                })
                put(JSONObject().apply {
                    put("id", 78)
                    put("xPoint", 0.07090322)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.23683053)
                    put("minAngle", 25)
                    put("maxAngle", 30)
                })
                put(JSONObject().apply {
                    put("id", 79)
                    put("xPoint", 0.031138225)
                    put("yPoint", -0.019210128)
                    put("zPoint", -0.22391011)
                    put("minAngle", 20)
                    put("maxAngle", 114)
                })

            }





            // Wrap it in a root JSON object
            val rootObject = JSONObject().apply {
                put("arcs", arcsArray);
                put("showRingInSequence",false);
                put("showBulletCoordinates",true);
                put("bulletCoordinates", bulletCoordinatesArray);
            }

            // Write to file (pretty print)
            savedFile.writeText(rootObject.toString(4))

            Log.d("makeRingConfigJsonFile", "ringConfig.json created at: ${savedFile.absolutePath}")


        } catch (e: Exception) {
            Log.e("makeRingConfigJsonFile", "Error creating JSON file: ${e.message}")
        }
    }


    private fun checkShowCoordinates(){
        val jsonFile = File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/ringConfig.json")
        if (!jsonFile.exists()) return
        val jsonObject = JSONObject(jsonFile.readText().trim())
        if (!jsonObject.has("showBulletCoordinates")) return
        val showCoords = jsonObject.getBoolean("showBulletCoordinates")
        showDynamicPoints=showCoords
    }
    //this gets data from ringconfig.json file to show on ui

    private fun loadsRingConfigFromJson(){
        Log.d("loadsRingConfigFromJson", "loadsRingConfigFromJson function called")
        val jsonFile = File(Environment.getExternalStorageDirectory(), "OpenLIFU-Config/ringConfig.json")
        if (!jsonFile.exists()) return

        val jsonObject = JSONObject(jsonFile.readText().trim())
        if (!jsonObject.has("arcs")) return





        ringPrametersList.clear()

        val arcsArray = jsonObject.getJSONArray("arcs")

        for (i in 0 until arcsArray.length()) {
            val arcObject = arcsArray.getJSONObject(i)
            ringPrametersList.add(ArcConfig(
                type = arcObject.getString("type"),
                radius = arcObject.getDouble("radius"),
                bulletCount = arcObject.getInt("bulletCount"),
                upDown = arcObject.getDouble("upDown"),
                closeFar = arcObject.getDouble("closeFar") ,
                minAngle = arcObject.getInt("minAngle"),
                maxAngle = arcObject.getInt("maxAngle"),

                ))
        }

        Log.d("loadsRingConfigFromJson", "added conifg")

        ringPrametersList.sortBy { it.upDown }



    }



    private fun setupDotsIndicator() {
        dots = ArrayList()
        dotsContainer = findViewById(R.id.dotsContainer)
        dotsContainer.removeAllViews()

        if (ringPrametersList.size > 1) { // Only show dots if there are multiple items
            for (i in ringPrametersList.indices) {
                dots.add(ImageView(this).apply {
                    setImageResource(if (i == 0) R.drawable.dot_indicator_selected else R.drawable.dot_indicator)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(8, 0, 8, 0)
                    layoutParams = params
                })
                dotsContainer.addView(dots[i])
            }
        }
    }

    private fun setupRecyclerViewWithDots() {
        val recyclerView = findViewById<RecyclerView>(R.id.parametersRecyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount

                if (visibleItemCount > 0) {
                    updateDots(firstVisibleItemPosition)
                }
            }
        })
    }

    private fun updateDots(currentPosition: Int) {
        for (i in dots.indices) {
            dots[i].setImageResource(if (i == currentPosition)
                R.drawable.dot_indicator_selected
            else
                R.drawable.dot_indicator
            )
        }
    }



}
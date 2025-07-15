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
        val qrIconButton=findViewById<ImageButton>(R.id.qrIconButton);
        val startCaptureButton=findViewById<Button>(R.id.startCaptureButton);
        newCaptureCheckbox=findViewById<CheckBox>(R.id.newCaptureCheckbox)
        val referenceNumberEditText = findViewById<EditText>(R.id.scanIDInputText)

        val fetchedQRText=intent.getStringExtra("QR_TEXT")
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val doNotShowInfoPref = prefs.getBoolean("do_not_show_info", false)

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

        qrIconButton.setOnClickListener {
            Log.d("QR activity","Launched")
            val intent = Intent(this, QrActivity::class.java)
            startActivity(intent)
        }









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
                    put("xPoint", -0.042076137)
                    put("yPoint", 0.043586373)
                    put("zPoint", -0.30080715)
                })
                put(JSONObject().apply {
                    put("id", 1)
                    put("xPoint", -0.18473463)
                    put("yPoint", 0.043586373)
                    put("zPoint", -0.4044546)
                })
                put(JSONObject().apply {
                    put("id", 2)
                    put("xPoint", -0.13024393)
                    put("yPoint", 0.043586373)
                    put("zPoint", -0.5721597)
                })
                put(JSONObject().apply {
                    put("id", 3)
                    put("xPoint", 0.046091657)
                    put("yPoint", 0.043586373)
                    put("zPoint", -0.5721597)
                })
                put(JSONObject().apply {
                    put("id", 4)
                    put("xPoint", 0.10058235)
                    put("yPoint", 0.043586373)
                    put("zPoint", -0.4044546)
                })
                put(JSONObject().apply {
                    put("id", 5)
                    put("xPoint", -0.04024834)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.27902624)
                })
                put(JSONObject().apply {
                    put("id", 6)
                    put("xPoint", -0.13429397)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.30958351)
                })
                put(JSONObject().apply {
                    put("id", 7)
                    put("xPoint", -0.19241738)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.38958353)
                })
                put(JSONObject().apply {
                    put("id", 8)
                    put("xPoint", -0.19241738)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.48846895)
                })
                put(JSONObject().apply {
                    put("id", 9)
                    put("xPoint", -0.13429397)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.5684689)
                })
                put(JSONObject().apply {
                    put("id", 10)
                    put("xPoint", -0.04024834)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.5990262)
                })
                put(JSONObject().apply {
                    put("id", 11)
                    put("xPoint", 0.053797297)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.5684689)
                })
                put(JSONObject().apply {
                    put("id", 12)
                    put("xPoint", 0.11192069)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.48846895)
                })
                put(JSONObject().apply {
                    put("id", 13)
                    put("xPoint", 0.11192069)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.38958353)
                })
                put(JSONObject().apply {
                    put("id", 14)
                    put("xPoint", 0.053797297)
                    put("yPoint", 0.005404312)
                    put("zPoint", -0.30958351)
                })
                put(JSONObject().apply {
                    put("id", 15)
                    put("xPoint", -0.038420543)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.23724538)
                })
                put(JSONObject().apply {
                    put("id", 16)
                    put("xPoint", -0.15009974)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.27353215)
                })
                put(JSONObject().apply {
                    put("id", 17)
                    put("xPoint", -0.21912128)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.36853215)
                })
                put(JSONObject().apply {
                    put("id", 18)
                    put("xPoint", -0.21912128)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.4859586)
                })
                put(JSONObject().apply {
                    put("id", 19)
                    put("xPoint", -0.15009974)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.5809586)
                })
                put(JSONObject().apply {
                    put("id", 20)
                    put("xPoint", -0.038420543)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.6172454)
                })
                put(JSONObject().apply {
                    put("id", 21)
                    put("xPoint", 0.07325865)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.5809586)
                })
                put(JSONObject().apply {
                    put("id", 22)
                    put("xPoint", 0.14228019)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.4859586)
                })
                put(JSONObject().apply {
                    put("id", 23)
                    put("xPoint", 0.14228019)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.36853215)
                })
                put(JSONObject().apply {
                    put("id", 24)
                    put("xPoint", 0.07325865)
                    put("yPoint", -0.032777756)
                    put("zPoint", -0.27353215)
                })
                put(JSONObject().apply {
                    put("id", 25)
                    put("xPoint", -0.03659274)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.22546446)
                })
                put(JSONObject().apply {
                    put("id", 26)
                    put("xPoint", -0.14827193)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.26175123)
                })
                put(JSONObject().apply {
                    put("id", 27)
                    put("xPoint", -0.21729347)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.35675123)
                })
                put(JSONObject().apply {
                    put("id", 28)
                    put("xPoint", -0.21729347)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.4741777)
                })
                put(JSONObject().apply {
                    put("id", 29)
                    put("xPoint", -0.14827193)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.5691777)
                })
                put(JSONObject().apply {
                    put("id", 30)
                    put("xPoint", -0.03659274)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.60546446)
                })
                put(JSONObject().apply {
                    put("id", 31)
                    put("xPoint", 0.07508646)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.5691777)
                })
                put(JSONObject().apply {
                    put("id", 32)
                    put("xPoint", 0.144108)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.4741777)
                })
                put(JSONObject().apply {
                    put("id", 33)
                    put("xPoint", 0.144108)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.35675123)
                })
                put(JSONObject().apply {
                    put("id", 34)
                    put("xPoint", 0.07508646)
                    put("yPoint", -0.07095983)
                    put("zPoint", -0.26175123)
                })


            }





            // Wrap it in a root JSON object
            val rootObject = JSONObject().apply {
                put("arcs", arcsArray);
                put("showRingInSequence",false);
                put("showBulletCoordinates",false);
                put("bulletCoordinates", bulletCoordinatesArray);
            }

            // Write to file (pretty print)
            savedFile.writeText(rootObject.toString(4))

            Log.d("makeRingConfigJsonFile", "ringConfig.json created at: ${savedFile.absolutePath}")


        } catch (e: Exception) {
            Log.e("makeRingConfigJsonFile", "Error creating JSON file: ${e.message}")
        }
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
package com.example.facedetectionar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException

class ReferenceInputActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reference_input)

        // Hide the status bar
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        // Log app startup
        LogFileUtil.appendLog("App started")

        val referenceNumberEditText = findViewById<EditText>(R.id.referenceNumberEditText)
        val submitButton = findViewById<Button>(R.id.submitButton)

        // Check required permissions
        if (!hasAllPermissions()) {
            requestPermissions()
        }

        // Check for All Files Access permission on Android 11+
        checkAllFilesAccessPermission()

        // Check and ensure ARCore availability
        checkAndInstallARCore()

        // Handle submit button click
        submitButton.setOnClickListener {
            val referenceNumber = referenceNumberEditText.text.toString().trim()

            if (referenceNumber.isEmpty()) {
                showToastAndLog("Please enter a reference number")
            } else if (hasAllPermissions()) {
                LogFileUtil.appendLog("Moving to face detection screen")
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
        val intent = Intent(this, InstructionsActivity::class.java)
        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
        startActivity(intent)
        finish() // Close this activity
    }

    // Show a toast and log a message
    private fun showToastAndLog(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        LogFileUtil.appendLog(message)
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

}

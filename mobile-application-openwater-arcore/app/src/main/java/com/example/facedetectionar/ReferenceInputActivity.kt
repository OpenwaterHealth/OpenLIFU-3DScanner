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
        val intent = Intent(this, FaceDetectionActivity::class.java)
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
}

package com.example.facedetectionar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class UsbScreenActivity : AppCompatActivity() {

    private lateinit var usbStatusTextView: TextView
    private lateinit var usbIcon: ImageView
    private lateinit var imageCountTextView: TextView
    private lateinit var successMessageTextView: TextView
    private lateinit var successImageView: ImageView
    private lateinit var backToHomeButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private var isUsbConnected = false
    private var referenceNumber: String = "REFNO"

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Log.d("USB Receiver", "Action: ${intent.action}")
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    isUsbConnected = true
                    //usbStatusTextView.text = "USB Connected!"
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    isUsbConnected = false
                    //usbStatusTextView.text = "USB Disconnected!"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_screen)

        usbStatusTextView = findViewById(R.id.usbStatusTextView)
        usbIcon = findViewById(R.id.usbIcon)
        imageCountTextView = findViewById(R.id.imageCountTextView)
        successMessageTextView = findViewById(R.id.successMessageTextView)
        successImageView = findViewById(R.id.successImageView)
        backToHomeButton = findViewById(R.id.backToHomeButton)

        // Hide success elements initially
        successMessageTextView.visibility = View.GONE
        successImageView.visibility = View.GONE
        backToHomeButton.visibility = View.GONE

        // Get the reference number from intent
        referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "REFNO"

        // Register USB receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)

        // Check USB connection status and image directory continuously
        handler.post(updateStatusRunnable)

        // Back to Home button click listener
        backToHomeButton.setOnClickListener {
            val intent = Intent(this, ReferenceInputActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Close this activity to prevent it from being in the back stack
        }

    }

    private val updateStatusRunnable = object : Runnable {
        override fun run() {
            // Check USB connection
            checkUsbConnection()

            // Check total captured images
            val totalImages = getTotalCapturedImages(referenceNumber)
            if (totalImages > 0) {
                imageCountTextView.text = "Total Captured Images: $totalImages"
            } else {
                showSuccessMessage()
            }

            // Re-run this task after 500 milliseconds
            handler.postDelayed(this, 500)
        }
    }

    private fun getTotalCapturedImages(referenceNumber: String): Int {
        // Define the custom directory path
        val imageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        if (!imageDir.exists() || !imageDir.isDirectory) return 0

        // Filter files based on the reference number prefix and ".jpeg" extension
        return imageDir.listFiles()?.count { it.name.startsWith(referenceNumber) && it.name.endsWith(".jpeg") } ?: 0
    }

    private fun checkUsbConnection() {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        isUsbConnected = usbManager.deviceList.isNotEmpty()
        val devices = usbManager.deviceList
        //Log.d("USB Devices", devices.toString())
        //usbStatusTextView.text = if (isUsbConnected) "USB Connected!" else "USB Disconnected!"
    }

    private fun showSuccessMessage() {
        // Display success message and hide other elements
        usbStatusTextView.visibility = View.GONE
        usbIcon.visibility = View.GONE;
        imageCountTextView.visibility = View.GONE
        successMessageTextView.visibility = View.VISIBLE
        successImageView.visibility = View.VISIBLE
        backToHomeButton.visibility = View.VISIBLE
        successMessageTextView.text = "Transferred Successfully"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister USB receiver
        unregisterReceiver(usbReceiver)

        // Stop the handler
        handler.removeCallbacks(updateStatusRunnable)
    }
}

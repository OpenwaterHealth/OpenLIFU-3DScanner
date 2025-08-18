package com.example.facedetectionar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class UsbScreenActivity : AppCompatActivity() {

    private lateinit var imageCountTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isUsbConnected = false
    private var isStorageMounted = false
    private var referenceNumber: String = "REFNO"
    private var totalImageCount: String = "00"



    // detects when usb is connected or disconnected
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    Log.d("USB_STATE", "Power connected")
                    checkUsbAndStorage()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    Log.d("USB_STATE", "Power disconnected")
                    isUsbConnected = false
                    isStorageMounted = false
                    updateUSBconnectionText()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usb_screen)
        val readyForTransferText = findViewById<TextView>(R.id.readyForTransferText)
        val usbOkButton = findViewById<Button>(R.id.usbOkButton)

        usbOkButton.setOnClickListener {
            val intent = Intent(this, welcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "REFNO"
        totalImageCount = intent.getStringExtra("TOTAL_IMAGE_COUNT") ?: "00"
        readyForTransferText.text = "Ready for Transfer Scan $referenceNumber"
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, filter)

        handler.post(updateStatusRunnable)
    }

    private val updateStatusRunnable = object : Runnable {
        override fun run() {
            checkUsbAndStorage()
            updateUSBconnectionText()

            // Safely check for images only when storage is mounted
            if (isUsbConnected && isStorageMounted) {
                try {
                    checkFolderExistance(referenceNumber)
                    
                } catch (e: Exception) {
                    Log.e("ImageCheck", "Error reading images: ${e.message}")
                }
            }

            handler.postDelayed(this, 1000)
        }
    }

    private fun checkUsbAndStorage() {
        val batteryStatusIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        isUsbConnected = plugged == BatteryManager.BATTERY_PLUGGED_USB

        val state = Environment.getExternalStorageState()
        isStorageMounted = state == Environment.MEDIA_MOUNTED


    }



    //function checks weather image folder exists or not
    private fun checkFolderExistance(referenceNumber: String) {
         val imageFolderName="${referenceNumber}"
         val folder = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner/$imageFolderName")
         if (!folder.exists()) {
             val usbStatusText = findViewById<TextView>(R.id.usbStatusText)
            usbStatusText.text = "Capture has been transferred successfully !"

        }

    }

    private fun updateUSBconnectionText() {
        val usbStatusText = findViewById<TextView>(R.id.usbStatusText)
        val usbConnectedIconImage = findViewById<ImageView>(R.id.usbConnectedIconImage)
        val usbDisConnectedIconImage = findViewById<ImageView>(R.id.usbDisConnectedIconImage)

        if (isUsbConnected && isStorageMounted) {
            usbDisConnectedIconImage.visibility = View.GONE
            usbConnectedIconImage.visibility = View.VISIBLE
            usbStatusText.text = "Connected"
        } else if (isUsbConnected) {
            usbConnectedIconImage.visibility = View.GONE
            usbDisConnectedIconImage.visibility = View.VISIBLE
            usbStatusText.text = "Connected"
        } else {
            usbConnectedIconImage.visibility = View.GONE
            usbDisConnectedIconImage.visibility = View.VISIBLE
            usbStatusText.text = "Disconnected"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerReceiver)
        handler.removeCallbacks(updateStatusRunnable)
    }
}

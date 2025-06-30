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



    private lateinit var imageCountTextView: TextView



    private val handler = Handler(Looper.getMainLooper())
    private var isUsbConnected = false
    private var referenceNumber: String = "REFNO"

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Log.d("USB Receiver", "Action: ${intent.action}")
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    isUsbConnected = true


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


        val readyForTransferText = findViewById<TextView>(R.id.readyForTransferText)


        val usbOkButton = findViewById<Button>(R.id.usbOkButton)


        usbOkButton.setOnClickListener {
            val intent= Intent(this, welcomeActivity::class.java)
            startActivity(intent)
            finish()
        }






        // Get the reference number from intent
        referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "REFNO"
        readyForTransferText.text="Ready for Transfer Scan ${referenceNumber}"

        // Register USB receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)

        // Check USB connection status and image directory continuously
        handler.post(updateStatusRunnable)



    }

    private val updateStatusRunnable = object : Runnable {
        override fun run() {
            // Check USB connection
            checkUsbConnection()


            //update usb status ui
            updateUSBconnectionText()

            // Check total captured images
            val totalImages = getTotalCapturedImages(referenceNumber)
            if (totalImages > 0) {
                imageCountTextView.text = "Total Captured Images: $totalImages"
            } else {
//                showSuccessMessage()
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



    override fun onDestroy() {
        super.onDestroy()
        // Unregister USB receiver
        unregisterReceiver(usbReceiver)

        // Stop the handler
        handler.removeCallbacks(updateStatusRunnable)
    }


    //updating the ui based on usb connection
    private fun updateUSBconnectionText(){
        val usbStatusText = findViewById<TextView>(R.id.usbStatusText)
        val usbConnectedIconImage = findViewById<ImageView>(R.id.usbConnectedIconImage)
        val usbDisConnectedIconImage = findViewById<ImageView>(R.id.usbDisConnectedIconImage)
        if(isUsbConnected){
            usbDisConnectedIconImage.visibility=View.GONE;
            usbConnectedIconImage.visibility=View.VISIBLE;
            usbStatusText.text="Connected"
        }
        else{
            usbConnectedIconImage.visibility=View.GONE;
            usbDisConnectedIconImage.visibility=View.VISIBLE
            usbStatusText.text="Disconnected"
    }
}}

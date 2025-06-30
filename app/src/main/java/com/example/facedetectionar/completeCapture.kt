package com.example.facedetectionar

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.facedetectionar.Adapters.ImageDiscardAdapter
import com.example.facedetectionar.Modals.ImageDiscardModal
import com.example.facedetectionar.Adapters.OnImageClickListener
import org.json.JSONObject
import java.io.File
import kotlin.text.get

class completeCapture : AppCompatActivity() {

    private lateinit var adapter: ImageDiscardAdapter
    private lateinit var imageList: ArrayList<ImageDiscardModal>
    private lateinit var imageViewPreview: ImageView
    private var selectedPosition = 0
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_complete_capture)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val discardCaptureButton = findViewById<Button>(R.id.discardCaptureButton)
        val saveFinishButton = findViewById<Button>(R.id.completeSaveFinishButton)
        val RefNumTextCaptureComplete = findViewById<TextView>(R.id.RefNumTextCaptureComplete)
        val CaptureCountTextOnComplete = findViewById<TextView>(R.id.CaptureCountTextOnComplete)
        val referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "DEFAULT_REF"
        val imageCount = intent.getStringExtra("IMAGE_COUNT") ?: "N/A"
        val totalImageCount = intent.getStringExtra("TOTAL_IMAGE_COUNT") ?: "N/A"
        val btnPrevious = findViewById<ImageButton>(R.id.btnPreviousInCaptureComplete)
        val btnNext = findViewById<ImageButton>(R.id.btnNextInCaptureComplete)

        RefNumTextCaptureComplete.text = referenceNumber
        CaptureCountTextOnComplete.text = "$imageCount/$totalImageCount"

        imageViewPreview = findViewById(R.id.imageViewPreview)
        recyclerView = findViewById(R.id.recyclerViewDiscard)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        imageList = ArrayList()
        val imageFiles = getImagesForReference(referenceNumber)
        for (file in imageFiles) {
            imageList.add(ImageDiscardModal(file))
        }

        adapter = ImageDiscardAdapter(imageList, this, object : OnImageClickListener {
            override fun onImageClick(imageFile: File) {
                Glide.with(this@completeCapture)
                    .load(imageFile)
                    .into(imageViewPreview)

                selectedPosition = imageList.indexOfFirst { it.imageFile == imageFile }
            }
        })

        recyclerView.adapter = adapter

        // Initially select the first image
        if (imageList.isNotEmpty()) {
            val firstImage = imageList[0].imageFile
            Glide.with(this).load(firstImage).into(imageViewPreview)
            adapter.setSelectedPosition(0)
        }

        // Previous button
        btnPrevious.setOnClickListener {
            if (selectedPosition > 0) {
                selectedPosition--
                updatePreviewSelection()
            }
        }

        // Next button
        btnNext.setOnClickListener {
            if (selectedPosition < imageList.size - 1) {
                selectedPosition++
                updatePreviewSelection()
            }
        }

        discardCaptureButton.setOnClickListener {
            val dialog = Dialog(this)
            val view = layoutInflater.inflate(R.layout.modal_capture_discard, null)
            val discardYesBtn = view.findViewById<Button>(R.id.discardYesBtn)
            val discardNoBtn = view.findViewById<Button>(R.id.discardNoBtn)

            discardYesBtn.setOnClickListener {
                deleteFolderAfterDiscard(this, referenceNumber)
            }
            discardNoBtn.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val marginInPx = (20 * metrics.density).toInt()
            val dialogWidth = screenWidth - (marginInPx * 2)

            dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }

        saveFinishButton.setOnClickListener {
            val dialog = Dialog(this)
            val view = layoutInflater.inflate(R.layout.modal_capture_save, null)
            val captureSaveNoButton = view.findViewById<Button>(R.id.captureSaveNoButton)
            val captureSaveYesButton = view.findViewById<Button>(R.id.captureSaveYesButton)


            val textFromString = getString(R.string.saveText, referenceNumber)
            val saveTextInModal = view.findViewById<TextView>(R.id.saveTextInModal)
            saveTextInModal.text = textFromString

            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val marginInPx = (20 * metrics.density).toInt()
            val dialogWidth = screenWidth - (marginInPx * 2)

            dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            captureSaveYesButton.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(this, UsbScreenActivity::class.java)
                intent.putExtra("REFERENCE_NUMBER", referenceNumber)
                startActivity(intent)
            }

            captureSaveNoButton.setOnClickListener {
                dialog.dismiss()
            }
        }
    }



    private fun updatePreviewSelection() {
        if (selectedPosition in imageList.indices) {
            val imageFile = imageList[selectedPosition].imageFile
            Glide.with(this).load(imageFile).into(imageViewPreview)
            adapter.setSelectedPosition(selectedPosition)
            recyclerView.scrollToPosition(selectedPosition)
        }
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

    fun getImagesForReference(referenceNumber: String): List<File> {
        val imageList = mutableListOf<File>()
        val totalImages=getTotalImageCountFromJson()
        val imageFolderName="${referenceNumber}_${totalImages}"
        val folder = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner/$imageFolderName")

        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles { _, name ->
                name.lowercase().endsWith(".jpeg") ||
                        name.lowercase().endsWith(".jpg") ||
                        name.lowercase().endsWith(".png")
            }

            files?.sortedBy { it.name }?.let {
                imageList.addAll(it)
            }
        }

        return imageList
    }

    private fun deleteFolderAfterDiscard(context: Context, referenceNumber: String) {
        try {

            val totalImages=getTotalImageCountFromJson()
            val imageFolderName="${referenceNumber}_${totalImages}"
            val folder = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner/$imageFolderName")
            if (folder.exists() && folder.isDirectory) {
                deleteRecursive(folder)
                Log.d("DeleteFolder", "Deleted folder and images for reference: $referenceNumber")
                val intent = Intent(context, welcomeActivity::class.java)
                context.startActivity(intent)
                if (context is Activity) context.finish()
            } else {
                Log.w("DeleteFolder", "Folder does not exist: $referenceNumber")
            }
        } catch (e: Exception) {
            Log.e("DeleteFolder", "Error deleting folder: ${e.message}")
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { deleteRecursive(it) }
        }
        fileOrDirectory.delete()
    }



}



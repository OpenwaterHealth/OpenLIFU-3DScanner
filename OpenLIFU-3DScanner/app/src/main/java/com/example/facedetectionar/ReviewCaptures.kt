package com.example.facedetectionar

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetectionar.Adapters.reviewListAdapter
import com.example.facedetectionar.Modals.reviewDataModal
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReviewCaptures : AppCompatActivity(), reviewListAdapter.OnReviewClickListener {

    private lateinit var reviewRecycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var showReviewButton: Button
    private lateinit var backButton: Button
    private lateinit var adapter: reviewListAdapter
    private var referenceID: String = ""


    private val reviewList = ArrayList<reviewDataModal>()

    override fun onResume() {
        super.onResume()
        loadReviewData() //
    }


    override fun onCreate(savedInstanceState: Bundle?) {




        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_review_captures)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        reviewRecycler = findViewById(R.id.reviewRecycler)
        emptyView = findViewById(R.id.emptyView)
        showReviewButton = findViewById(R.id.showReviewScreenButton)
        backButton = findViewById(R.id.backReviewScreen)


        // Setup RecyclerView
        adapter = reviewListAdapter(reviewList, this, this)
        reviewRecycler.layoutManager = LinearLayoutManager(this)
        reviewRecycler.adapter = adapter

        // Load actual data from folders
        loadReviewData()

        // Show Review button
        showReviewButton.setOnClickListener {
            if (referenceID.isEmpty()) {
                Log.d("Aryan", "showReviewButton error")
                if(reviewList.size ==0){
                    Toast.makeText(this, "No capture found for review", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "Select one review", Toast.LENGTH_SHORT).show()
                }

            } else {
                val intent = Intent(this, ImagePreview::class.java)
                intent.putExtra("REFERENCE_ID", referenceID)
                startActivity(intent)
            }
        }




        backButton.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this, welcomeActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

    private fun loadReviewData() {
        reviewList.clear()
        reviewList.addAll(getReviewDataList())
        adapter.notifyDataSetChanged()
    }

    private fun getReviewDataList(): List<reviewDataModal> {
        val reviewList = mutableListOf<reviewDataModal>()
        val rootFolder = File(Environment.getExternalStorageDirectory(), "OpenLIFU-3DScanner")

        if (rootFolder.exists() && rootFolder.isDirectory) {
            val referenceFolders = rootFolder.listFiles { file -> file.isDirectory }

            referenceFolders?.forEach { folder ->
                val imageCount = folder.listFiles { file ->
                    val name = file.name.lowercase()
                    name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                }?.size ?: 0

                val lastModified = Date(folder.lastModified())
                val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(lastModified)



                if (imageCount > 0) {
                    reviewList.add(
                        reviewDataModal(
                            date = formattedDate,
                            id = folder.name,
                            count = imageCount
                        )
                    )
                } else {
                    // If folder has 0 images, delete the folder and its contents
                    folder.deleteRecursively()
//                    Log.d("ReviewCleanup", "Deleted empty folder: ${folder.name}")
                }
            }
        }

        return reviewList.sortedByDescending { it.date }
    }

    override fun onReviewClick(item: reviewDataModal) {
        // Toast.makeText(this, "Clicked: ID = ${item.id}", Toast.LENGTH_SHORT).show()


        referenceID = item.id;





    }

}

package com.example.facedetectionar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {
    private var referenceNumber: String = "DEFAULT_REF" // Default reference number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        // Hide the status bar
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        val instructionVideo = findViewById<VideoView>(R.id.instructionVideo)
        val nextButton = findViewById<Button>(R.id.nextButton)

        // Set the reference number from the previous screen
        referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: "DEFAULT_REF"

        // Set up the instruction video
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.instruction_video}") // Place the video in the raw folder
        instructionVideo.setVideoURI(videoUri)
        instructionVideo.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true // Loop the video
        }
        instructionVideo.start()

        // Handle Next button click
        nextButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("REFERENCE_NUMBER", referenceNumber)
            startActivity(intent)
            finish() // Close the current activity
        }
    }
}

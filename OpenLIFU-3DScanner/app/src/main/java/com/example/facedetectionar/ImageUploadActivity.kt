package com.example.facedetectionar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.facedetectionar.api.repository.ReconstructionRepository
import com.example.facedetectionar.api.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import io.github.sceneview.utils.setKeepScreenOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ImageUploadActivity : AppCompatActivity() {

    @Inject
    lateinit var reconstructionRepository: ReconstructionRepository
    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var textTitle: TextView
    private lateinit var textStatus: TextView
    private lateinit var textDescription: TextView
    private lateinit var textCreditsAvailable: TextView
    private lateinit var textCreditsRequired: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonHome: Button
    private lateinit var buttonStart: Button
    private lateinit var buttonRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_upload)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setKeepScreenOn(true)

        textTitle = findViewById(R.id.title)
        textStatus = findViewById(R.id.status)
        progressBar = findViewById(R.id.progress_bar)
        textDescription = findViewById(R.id.description)
        textCreditsAvailable = findViewById(R.id.creditsAvailable)
        textCreditsRequired = findViewById(R.id.creditsRequired)
        buttonHome = findViewById(R.id.returnHomeButton)
        buttonStart = findViewById(R.id.startButton)
        buttonRefresh = findViewById(R.id.refreshButton)
        val loadingLayout = findViewById<View>(R.id.layoutLoading)
        buttonStart.isEnabled = false

        subscribeToUserCredits()
        subscribeToImageUploadingProgress()

        buttonStart.setOnClickListener {
            buttonStart.isEnabled = false
            loadingLayout.visibility = View.VISIBLE

            lifecycleScope.launch {
                val photoscanId = reconstructionRepository.startReconstruction()
                loadingLayout.visibility = View.GONE
                if (photoscanId != null) {
                    val intent = Intent(this@ImageUploadActivity, ReconstructionActivity::class.java)
                        .putExtra(ReconstructionActivity.EXTRA_PHOTOSCAN_ID, photoscanId)
                    startActivity(intent)
                    finish()
                } else {
                    buttonStart.isEnabled = true
                }
            }
        }

        buttonRefresh.setOnClickListener {
            buttonRefresh.isEnabled = false
            lifecycleScope.launch {
                userRepository.refreshUserInfo()
                refreshUI()
            }
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(application, welcomeActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
            finish()
        }

        lifecycleScope.launch {
            userRepository.refreshUserInfo()
        }
    }

    private fun subscribeToImageUploadingProgress() {
        lifecycleScope.launch {
            reconstructionRepository.getImageUploadProgress().flowWithLifecycle(lifecycle).collect { progress ->
                if (progress == null) return@collect

                if (progress.progress < 100) {
                    textTitle.text = getString(R.string.uploading)
                    textTitle.setTextColor(getColor(R.color.white))
                    textStatus.text = getString(
                        R.string.uploading_images_n_n,
                        progress.uploadedImages,
                        progress.totalImages
                    )
                    buttonStart.isEnabled = false
                } else if (progress.progress == 100) {
                    refreshUI()
                } else if (progress.failed) {
                    textTitle.text = getString(R.string.upload_failed)
                    textTitle.setTextColor(getColor(R.color.red))
                }
                progressBar.setProgress(progress.progress, true)
            }
        }
        lifecycleScope.launch {
            reconstructionRepository.uploadImages()
        }
    }

    private fun refreshUI() {
        val uploadFinished = reconstructionRepository.getImageUploadProgress().value?.progress == 100
        val enoughCredits = (userRepository.getUserInfo().value?.credits ?: 0) >= REQUIRED_CREDITS

        when {
            uploadFinished && enoughCredits -> {
                textTitle.text = getString(R.string.ready)
                textTitle.setTextColor(getColor(R.color.white))
                textStatus.text = getString(R.string.ready_to_reconstruct)
                textDescription.text = getString(R.string.reconstruction_can_also_be_started_later_from_the_review_captures_screen)
                buttonRefresh.visibility = View.INVISIBLE
                buttonStart.visibility = View.VISIBLE
                buttonStart.isEnabled = true
            }

            uploadFinished && !enoughCredits -> {
                textTitle.setTextColor(getColor(R.color.red))
                textTitle.text = getString(R.string.not_enough_credits)
                textStatus.text = getString(R.string.cannot_start_reconstruction)

                textDescription.text = getString(
                    R.string.contact_sales_to_purchase_additional_compute_credits_images_will_be_saved_and_can_be_reconstructed_later
                )
                buttonRefresh.isEnabled = true
                buttonRefresh.visibility = View.VISIBLE
                buttonStart.visibility = View.INVISIBLE
            }
        }
    }

    private fun subscribeToUserCredits() {
        lifecycleScope.launch {
            userRepository.getUserInfo().flowWithLifecycle(lifecycle).collect {
                val credits = it?.credits ?: 0
                textCreditsAvailable.text = getString(R.string.credits_available_n, credits)
                textCreditsRequired.text = getString(R.string.credits_required_n, REQUIRED_CREDITS)

                val color = if (credits < REQUIRED_CREDITS) R.color.red else R.color.light_green
                textCreditsAvailable.setTextColor(getColor(color))
                textCreditsRequired.setTextColor(getColor(color))

                refreshUI()
            }
        }
    }

    companion object {
        private const val REQUIRED_CREDITS = 1
    }
}
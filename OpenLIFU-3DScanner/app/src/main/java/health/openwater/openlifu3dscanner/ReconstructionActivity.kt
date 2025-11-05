package health.openwater.openlifu3dscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import health.openwater.openlifu3dscanner.api.dto.Photoscan
import health.openwater.openlifu3dscanner.api.repository.CloudRepository
import health.openwater.openlifu3dscanner.api.repository.UserRepository
import health.openwater.openlifu3dscanner.dialogs.PhotoscanDownloadDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReconstructionActivity : AppCompatActivity() {

    @Inject
    lateinit var cloudRepository: CloudRepository
    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var textTitle: TextView
    private lateinit var textDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonHome: Button
    private lateinit var buttonDownload: Button

    private var photoscan: Photoscan? = null
    private var photoscanId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reconstruction)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textTitle = findViewById(R.id.title)
        progressBar = findViewById(R.id.progress_bar)
        textDescription = findViewById(R.id.description)
        buttonHome = findViewById(R.id.returnHomeButton)
        buttonDownload = findViewById(R.id.downloadButton)
        buttonDownload.isEnabled = false

        photoscanId = intent.getLongExtra(EXTRA_PHOTOSCAN_ID, 0)

        buttonHome.setOnClickListener {
            startActivity(Intent(application, welcomeActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            })
            finish()
        }

        supportFragmentManager.setFragmentResultListener(PhotoscanDownloadDialog.TAG, this) { requestKey, bundle ->
            val intent = Intent(this, UsbScreenActivity::class.java)
                .putExtra("REFERENCE_NUMBER", cloudRepository.currentReferenceNumber)
                .putExtra("TOTAL_IMAGE_COUNT", cloudRepository.totalImageCount)
            startActivity(intent)
            finish()
        }

        buttonDownload.setOnClickListener {
            photoscan?.let {
                val dialog = PhotoscanDownloadDialog(it.id)
                dialog.show(supportFragmentManager, PhotoscanDownloadDialog::class.simpleName)
            }
        }

        subscribeToProgress()
        lifecycleScope.launch {
            photoscan = cloudRepository.startReconstructionProgressListener(photoscanId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cloudRepository.stopReconstructionProgressListener(photoscanId)
    }

    private fun subscribeToProgress() {
        lifecycleScope.launch {
            cloudRepository.getReconstructionProgress().flowWithLifecycle(lifecycle).collect { progress ->
                if (progress == null) return@collect

                when (progress.status) {
                    "FINISHED" -> {
                        textTitle.text = getString(R.string.reconstruction_complete)
                        textDescription.text =
                            getString(R.string.results_can_also_be_downloaded_later)

                        buttonDownload.isEnabled = true
                    }

                    "FAILED" -> {
                        textTitle.text = getString(R.string.reconstruction_failed)
                        textTitle.setTextColor(getColor(R.color.red))
                        buttonDownload.isEnabled = true
                    }
                }

                progressBar.setProgress(progress.progress, true)
            }
        }
    }

    companion object {
        const val EXTRA_PHOTOSCAN_ID = "EXTRA_PHOTOSCAN_ID"
    }
}
package health.openwater.openlifu3dscanner

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import health.openwater.openlifu3dscanner.databinding.ActivityCameraBinding
import health.openwater.openlifu3dscanner.utils.CameraManager
import health.openwater.openlifu3dscanner.viewmodel.PhotoCaptureViewModel
import io.github.sceneview.utils.setKeepScreenOn
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CameraActivity : BaseActivity() {

    private lateinit var binding: ActivityCameraBinding

    private val viewModel: PhotoCaptureViewModel by viewModels()
    private var cameraManager: CameraManager? = null
    private lateinit var referenceNumber: String

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cameraManager = CameraManager(this, this, binding.cameraPreview) {
            viewModel.onFacesDetected(it)
        }.also {
            viewModel.setCameraManager(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setKeepScreenOn(true)

        binding = ActivityCameraBinding.inflate(
            LayoutInflater.from(this), null, false
        )
        setContentView(binding.root)
        applyWindowInsets(R.id.main, displayCutout = false)

        referenceNumber = intent?.getStringExtra("REFERENCE_NUMBER") ?: ""

        viewModel.setReferenceNumber(referenceNumber)

        lifecycleScope.launch {
            viewModel.getFaceDetectionCompleteFlow().flowWithLifecycle(lifecycle).collect {
                if (it) {
                    cameraManager?.stopFaceDetection()
                    binding.fragmentFaceDetection.visibility = View.GONE
                    binding.fragmentPhotoCapture.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.getCaptureCompleteFlow().flowWithLifecycle(lifecycle).collect {
                if (it) {
                    navigateToCaptureCompleteScreen()
                }
            }
        }
    }

    fun navigateToCaptureCompleteScreen() {
        val intent = Intent(this, completeCapture::class.java)
        intent.putExtra("REFERENCE_NUMBER", referenceNumber)
        intent.putExtra("IMAGE_COUNT", viewModel.getCapturedImageCount().toString())
        intent.putExtra("TOTAL_IMAGE_COUNT", viewModel.getTotalImageCount().toString())

        startActivity(intent)
        finish()
    }

    fun navigateWelcomeScreen() {
        val intent = Intent(this, welcomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
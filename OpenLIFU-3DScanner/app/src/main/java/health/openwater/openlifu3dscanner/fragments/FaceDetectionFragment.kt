package health.openwater.openlifu3dscanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import health.openwater.openlifu3dscanner.CameraActivity
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.databinding.FragmentFaceDetectionBinding
import health.openwater.openlifu3dscanner.viewmodel.PhotoCaptureViewModel
import kotlinx.coroutines.launch

class FaceDetectionFragment : BaseFragment() {

    private val viewModel: PhotoCaptureViewModel by activityViewModels()

    private var _binding: FragmentFaceDetectionBinding? = null
    private val binding: FragmentFaceDetectionBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaceDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyWindowInsets(binding.root)

        lifecycleScope.launch {
            viewModel.getFacesFlow().flowWithLifecycle(lifecycle).collect {
                binding.faceContourView.setFaces(it)
            }
        }
        lifecycleScope.launch {
            viewModel.getFaceDetectedFlow().flowWithLifecycle(lifecycle).collect {
                binding.title.text = getString(if (it) R.string.face_detected else R.string.scan_your_head)
                binding.footer.visibility = if (it) View.INVISIBLE else View.VISIBLE
            }
        }

        binding.buttonBack.setOnClickListener {
            viewModel.stopCapture(false)
            (requireActivity() as CameraActivity).navigateWelcomeScreen()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                _binding?.buttonBack?.performClick()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
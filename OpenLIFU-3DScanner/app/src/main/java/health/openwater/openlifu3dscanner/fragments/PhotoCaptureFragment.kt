package health.openwater.openlifu3dscanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import health.openwater.openlifu3dscanner.CameraActivity
import health.openwater.openlifu3dscanner.R
import health.openwater.openlifu3dscanner.databinding.FragmentPhotoCaptureBinding
import health.openwater.openlifu3dscanner.viewmodel.PhotoCaptureViewModel
import kotlinx.coroutines.launch

class PhotoCaptureFragment : BaseFragment() {

    private val viewModel: PhotoCaptureViewModel by activityViewModels()

    private var _binding: FragmentPhotoCaptureBinding? = null
    private val binding: FragmentPhotoCaptureBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyWindowInsets(binding.root)

        Glide.with(this)
            .asGif()
            .load(R.drawable.left)
            .into(binding.imageLeft)

        binding.buttonBack.setOnClickListener {
            viewModel.stopCapture(false)
            (requireActivity() as CameraActivity).navigateWelcomeScreen()
        }

        ViewCompat.setBackgroundTintList(binding.buttonDone, null)
        binding.buttonDone.setOnClickListener {
            viewModel.stopCapture(true)
            (requireActivity() as CameraActivity).navigateToCaptureCompleteScreen()
        }

        binding.buttonPlay.setOnClickListener {
            viewModel.startCapture()
            binding.buttonPlay.visibility = View.INVISIBLE
            binding.buttonPause.visibility = View.VISIBLE
        }

        binding.buttonPause.setOnClickListener {
            viewModel.stopCapture(false)
            binding.buttonPlay.visibility = View.VISIBLE
            binding.buttonPause.visibility = View.INVISIBLE
        }

        lifecycleScope.launch {
            viewModel.getCapturedImagesNumberFlow().flowWithLifecycle(lifecycle).collect {
                binding.progressBar.progress = it
                binding.progressBar.max = viewModel.getTotalImageCount()

                binding.textImages.text =
                    if (it > 0)
                        getString(R.string.n_images, it)
                    else
                        getString(R.string.zero_images)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    _binding?.buttonBack?.performClick()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopCapture(false)
    }

    override fun onResume() {
        super.onResume()
        _binding?.apply {
            buttonPlay.visibility = View.VISIBLE
            buttonPause.visibility = View.INVISIBLE
        }
    }
}
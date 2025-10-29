package com.example.facedetectionar.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.facedetectionar.R
import com.example.facedetectionar.api.dto.Photoscan
import com.example.facedetectionar.api.repository.CloudRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhotoscanDownloadDialog(
    private val photoscanId: Long
): DialogFragment() {
    @Inject
    lateinit var cloudRepository: CloudRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.modal_photoscan_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusText = requireView().findViewById<TextView>(R.id.status)
        val messageText = requireView().findViewById<TextView>(R.id.message)
        val progress = requireView().findViewById<ProgressBar>(R.id.progress)
        val continueButton = view.findViewById<Button>(R.id.continueBtn)

        continueButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(TAG, bundleOf(KEY_RESULT to true))
            dismiss()
        }

        lifecycleScope.launch {
            val scanName = download()

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                progress.visibility = View.GONE

                if (scanName != null) {
                    statusText.text = getString(R.string.download_successful)
                    statusText.setTextColor(requireView().context.getColor(R.color.light_green))

                    messageText.text = getString(R.string.scan_s, scanName)
                    messageText.visibility = View.VISIBLE
                } else {
                    statusText.text = getString(R.string.download_failed)
                    statusText.setTextColor(requireView().context.getColor(R.color.red))

                    messageText.text = getString(R.string.try_again_later)
                    messageText.visibility = View.VISIBLE
                }

                continueButton.isEnabled = true
            }
        }
    }

    private suspend fun download(): String? {
        val photoscan = cloudRepository.getPhotoscan(photoscanId) ?: return null
        val name = cloudRepository.getPhotocollection(photoscan.photocollectionId)?.name ?: return null
        val outputDir = cloudRepository.getImagesDir(name)
        if (!outputDir.exists()) outputDir.mkdirs()
        if (!cloudRepository.downloadPhotoscanZip(photoscan.id, name, outputDir))
            return null
        return name
    }

    companion object {
        val TAG = PhotoscanDownloadDialog::class.java.simpleName
        const val KEY_RESULT = "KEY_RESULT"
    }
}
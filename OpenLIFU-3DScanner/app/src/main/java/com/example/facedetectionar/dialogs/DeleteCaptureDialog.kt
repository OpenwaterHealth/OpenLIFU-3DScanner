package com.example.facedetectionar.dialogs

import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.facedetectionar.R
import com.example.facedetectionar.ReviewCapturesActivity
import com.example.facedetectionar.api.repository.CloudRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeleteCaptureDialog(
    private val referenceNumber: String,
    private val photocollectionId: Long
): DialogFragment() {
    @Inject
    lateinit var cloudRepository: CloudRepository
    private var state: State = State.INITIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.modal_delete_capture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deleteLocalOnlyButton = view.findViewById<Button>(R.id.deleteLocalOnlyBtn)
        val deleteBothButton = view.findViewById<Button>(R.id.deleteBothBtn)
        val deleteCloudOnlyButton = view.findViewById<Button>(R.id.deleteCloudOnlyBtn)
        val deleteButton = view.findViewById<Button>(R.id.deleteBtn)
        val cancelButton = view.findViewById<Button>(R.id.cancelBtn)
        val loadingProgress = view.findViewById<View>(R.id.loadingProgress)

        updateUI(view)

        deleteButton.setOnClickListener {
            loadingProgress.visibility = View.VISIBLE
            lifecycleScope.launch {
                when (state) {
                    State.DELETE_LOCAL_OFFLINE -> deleteLocal()
                    State.DELETE_LOCAL_ONLY -> deleteLocal()
                    State.DELETE_BOTH -> {
                        deleteLocal()
                        deleteOnCloud()
                    }
                    State.DELETE_CLOUD_ONLY -> deleteOnCloud()
                    else -> {
                        loadingProgress.visibility = View.GONE
                        return@launch
                    }
                }
                openReviewCapturesScreen()
                loadingProgress.visibility = View.GONE
            }
        }

        deleteLocalOnlyButton.setOnClickListener {
            state = State.DELETE_LOCAL_ONLY
            updateUI(view)
        }

        deleteBothButton.setOnClickListener {
            state = State.DELETE_BOTH
            updateUI(view)
        }

        deleteCloudOnlyButton.setOnClickListener {
            state = State.DELETE_CLOUD_ONLY
            updateUI(view)
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun deleteLocal() {
        val dir = cloudRepository.getImagesDir(referenceNumber)
        if (dir.exists()) dir.deleteRecursively()
    }

    private suspend fun deleteOnCloud() {
        cloudRepository.deletePhotocollection(photocollectionId)
    }

    private fun isDownloaded() = cloudRepository.getImagesDir(referenceNumber).exists()

    private fun updateUI(view: View) {
        val deleteLocalOnlyButton = view.findViewById<Button>(R.id.deleteLocalOnlyBtn)
        val deleteBothButton = view.findViewById<Button>(R.id.deleteBothBtn)
        val deleteCloudOnlyButton = view.findViewById<Button>(R.id.deleteCloudOnlyBtn)
        val deleteButton = view.findViewById<Button>(R.id.deleteBtn)
        val description = view.findViewById<TextView>(R.id.descriptionText)

        deleteLocalOnlyButton.visibility = View.GONE
        deleteBothButton.visibility = View.GONE
        deleteCloudOnlyButton.visibility = View.GONE

        when (state) {
            State.INITIAL -> {
                if (photocollectionId < 0) { // offline
                    state = State.DELETE_LOCAL_OFFLINE
                    updateUI(view)
                } else {
                    val downloaded = isDownloaded()
                    deleteLocalOnlyButton.isEnabled = downloaded
                    deleteBothButton.isEnabled = downloaded

                    deleteLocalOnlyButton.visibility = View.VISIBLE
                    deleteBothButton.visibility = View.VISIBLE
                    deleteCloudOnlyButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.GONE
                    description.text = getSpanned(R.string.delete_capture_description)
                }
            }
            State.DELETE_LOCAL_OFFLINE -> {
                description.text = getSpanned(R.string.delete_capture_description_local_only_offline)
                deleteButton.visibility = View.VISIBLE
            }
            State.DELETE_LOCAL_ONLY -> {
                description.text = getSpanned(R.string.delete_capture_description_local_only_online)
                deleteButton.visibility = View.VISIBLE
            }
            State.DELETE_BOTH -> {
                description.text = getSpanned(R.string.delete_capture_description_both)
                deleteButton.visibility = View.VISIBLE
            }
            State.DELETE_CLOUD_ONLY -> {
                description.text = getSpanned(R.string.delete_capture_description_cloud_only)
                deleteButton.visibility = View.VISIBLE
            }
        }
    }

    fun openReviewCapturesScreen() {
        dismiss()
        requireActivity().finish()
        startActivity(Intent(requireContext(), ReviewCapturesActivity::class.java))
    }

    private fun getSpanned(resId: Int): Spanned {
        return HtmlCompat.fromHtml(
            getString(resId),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}

private enum class State {
    INITIAL,
    DELETE_LOCAL_OFFLINE,
    DELETE_LOCAL_ONLY,
    DELETE_BOTH,
    DELETE_CLOUD_ONLY
}
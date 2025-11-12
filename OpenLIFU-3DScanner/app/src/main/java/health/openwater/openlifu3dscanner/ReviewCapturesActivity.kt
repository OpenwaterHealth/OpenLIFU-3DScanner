package health.openwater.openlifu3dscanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import health.openwater.openlifu3dscanner.Adapters.ReviewListAdapter
import health.openwater.openlifu3dscanner.viewmodel.ReviewCapturesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReviewCapturesActivity : BaseActivity() {

    private val viewmodel: ReviewCapturesViewModel by viewModels()

    private lateinit var reviewRecycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var showReviewButton: Button
    private lateinit var backButton: Button
    private lateinit var adapter: ReviewListAdapter
    private lateinit var showOnlineScansCheckbox: CheckBox
    private lateinit var loadingProgress: View
    private lateinit var tableHeader: View
    private lateinit var dateHeader: TextView

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_captures)
        applyWindowInsets(R.id.main, displayCutout = true)

        if (!hasAllPermissions()) {
            requestPermissions()
        }
        checkAllFilesAccessPermission()

        // Initialize views
        reviewRecycler = findViewById(R.id.reviewRecycler)
        emptyView = findViewById(R.id.emptyView)
        showReviewButton = findViewById(R.id.showReviewScreenButton)
        backButton = findViewById(R.id.backReviewScreen)
        showOnlineScansCheckbox = findViewById(R.id.showOnlineScansCheckbox)
        loadingProgress = findViewById(R.id.loadingProgress)
        tableHeader = findViewById(R.id.header)
        dateHeader = tableHeader.findViewById(R.id.dateText)

        tableHeader.visibility = View.INVISIBLE

        // Setup RecyclerView
        adapter = ReviewListAdapter(this, viewmodel)
        reviewRecycler.layoutManager = LinearLayoutManager(this)
        reviewRecycler.adapter = adapter

        // Show Review button
        showReviewButton.setOnClickListener {
            val intent = Intent(this, ImagePreview::class.java)
            intent.putExtra("REFERENCE_ID", viewmodel.getSelectedItemFlow().value?.id)
            intent.putExtra("PHOTOCOLLECTION_ID", viewmodel.getSelectedItemFlow().value?.photocollectionId)
            intent.putExtra("PHOTOSCAN_ID", viewmodel.getSelectedItemFlow().value?.photoscanId)

            startActivity(intent)
            finish()
        }

        // Back button
        backButton.setOnClickListener {
            val intent = Intent(this, welcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        showOnlineScansCheckbox.setOnCheckedChangeListener { _, _ ->
            loadData()
        }

        dateHeader.setOnClickListener {
            adapter.setSortOrder(!adapter.sortAsc)

            val drawable = AppCompatResources.getDrawable(
                this,
                if (adapter.sortAsc) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            )?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                setTint(ContextCompat.getColor(this@ReviewCapturesActivity, R.color.white))
            }
            dateHeader.setCompoundDrawablesRelative(drawable, null, null, null)
        }

        lifecycleScope.launch {
            viewmodel.getSelectedItemFlow().flowWithLifecycle(lifecycle).collect {
                showReviewButton.isEnabled = it != null
            }
        }

        lifecycleScope.launch {
            viewmodel.getDataFlow().flowWithLifecycle(lifecycle).collect {
                adapter.setData(it)
                if (it.isNotEmpty()) tableHeader.visibility = View.VISIBLE
            }
        }

        lifecycleScope.launch {
            viewmodel.getDownloadStatusChangeFlow().flowWithLifecycle(lifecycle).collect {
                if (it != null) adapter.notifyItemChanged(it)
            }
        }

        lifecycleScope.launch {
            viewmodel.getLoadingFlow().flowWithLifecycle(lifecycle).collect {
                loadingProgress.visibility = if (it) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadData() {
        viewmodel.loadReviewData(showOnlineScansCheckbox.isChecked)
    }

}

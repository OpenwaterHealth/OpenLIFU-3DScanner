package com.example.facedetectionar.Adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetectionar.Modals.ReviewData
import com.example.facedetectionar.Modals.Status
import com.example.facedetectionar.R
import com.example.facedetectionar.viewmodel.ReviewCapturesViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewListAdapter(
    private val context: Context,
    private val viewModel: ReviewCapturesViewModel
) : RecyclerView.Adapter<ReviewListAdapter.ReviewViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION
    var sortAsc = false
        private set

    private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    private val data = mutableListOf<ReviewData>()

    fun setData(list: List<ReviewData>) {
        data.clear()
        data.addAll(list)
        sortData()
        notifyDataSetChanged()
    }

    fun setSortOrder(ascending: Boolean) {
        sortAsc = ascending
        sortData()
        selectedPosition = RecyclerView.NO_POSITION
        viewModel.setSelectedItem(null)
        notifyItemRangeChanged(0, itemCount)
    }

    fun notifyItemChanged(item: ReviewData) {
        val idx = data.indexOfFirst {
            it.date == item.date &&
            it.photocollectionId == item.photocollectionId &&
            it.photoscanId == item.photoscanId
        }
        if (idx == -1) return
        data.removeAt(idx)
        data.add(idx, item)
        notifyItemChanged(idx)
    }

    private fun sortData() {
        val comparator = Comparator.comparing(ReviewData::date)
        if (sortAsc) {
            data.sortWith(comparator)
        } else {
            data.sortWith(comparator.reversed())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.review_layout, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = data[position]

        holder.dateText.text = dateFormatter.format(item.date)

        holder.countText.text = item.count.toString()
        val idText = item.id

        val baseName = idText.substringBeforeLast("_") // "testName"

        holder.idText.text = baseName

        holder.root.setBackgroundColor(
            context.getColor(
                when {
                    position == selectedPosition -> R.color.light_green
                    position % 2 == 0 -> R.color.gray
                    else -> R.color.light_gray
                }
            )
        )

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val oldPosition = selectedPosition
            selectedPosition = currentPosition

            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            viewModel.setSelectedItem(item)
        }

        holder.photoStatusImage.setImageDrawable(getDrawableRes(item.photoStatus))
        holder.meshStatusImage.setImageDrawable(getDrawableRes(item.meshStatus))

        stopLoadingAnimation(holder.photoStatusImage)
        stopLoadingAnimation(holder.meshStatusImage)

        if (item.photoStatus == Status.DOWNLOADING)
            startLoadingAnimation(holder.photoStatusImage)

        if (item.meshStatus == Status.DOWNLOADING)
            startLoadingAnimation(holder.meshStatusImage)

        holder.photoStatusImage.setOnClickListener {
            viewModel.downloadPhotocollection(item)
        }

        holder.meshStatusImage.setOnClickListener {
            viewModel.downloadPhotoscan(item)
        }

        holder.divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE
    }

    private fun getDrawableRes(status: Status): Drawable? {
        return when (status) {
            Status.LOCAL -> AppCompatResources.getDrawable(context,R.drawable.ic_checkbox)
            Status.CLOUD -> AppCompatResources.getDrawable(context,R.drawable.ic_cloud)
            Status.DOWNLOADING -> AppCompatResources.getDrawable(context,R.drawable.ic_spinner_progress)
            Status.UNKNOWN -> null
        }
    }

    private fun startLoadingAnimation(view: View) {
        val rotate = ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 360f).apply {
            duration = 1000L           // 1 second per rotation
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        rotate.start()
        view.setTag(view.id, rotate)
    }

    private fun stopLoadingAnimation(view: View) {
        (view.getTag(view.id) as? ObjectAnimator)?.cancel()
        view.rotation = 0f
    }

    override fun getItemCount(): Int = data.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.rootView
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val idText: TextView = itemView.findViewById(R.id.idText)
        val countText: TextView = itemView.findViewById(R.id.imageCountText)
        val photoStatusImage: ImageView = itemView.findViewById(R.id.photoStatusImage)
        val meshStatusImage: ImageView = itemView.findViewById(R.id.meshStatusImage)
        val divider: View = itemView.findViewById(R.id.divider)
    }
}

package com.example.facedetectionar.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetectionar.Modals.reviewDataModal
import com.example.facedetectionar.R

class reviewListAdapter(
    private val list: ArrayList<reviewDataModal>,
    private val context: Context,
    private val listener: OnReviewClickListener
) : RecyclerView.Adapter<reviewListAdapter.ReviewViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.review_layout, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = list[position]

        holder.dateText.text = item.date

        holder.countText.text = item.count.toString()
        val idText = context.getString(R.string.idInReview, item.id)

        val baseName = idText.substringBeforeLast("_") // "testName"


        holder.idText.text = baseName


        val backgroundRes=if (position == selectedPosition) R.drawable.review_green else R.drawable.review_white

        holder.reviewSingleRow.setBackgroundResource(
            backgroundRes
        )


        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val oldPosition = selectedPosition
            selectedPosition = currentPosition

            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            listener.onReviewClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val idText: TextView = itemView.findViewById(R.id.idText)
        val countText: TextView = itemView.findViewById(R.id.countText)
        val reviewSingleRow: LinearLayout=itemView.findViewById(R.id.reviewSingleRow)
    }

    interface OnReviewClickListener {
        fun onReviewClick(item: reviewDataModal)
    }
}

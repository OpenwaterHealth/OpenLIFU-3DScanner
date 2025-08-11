package com.example.facedetectionar.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.facedetectionar.Modals.ArcConfig
import com.example.facedetectionar.R

class ArcConfigAdapter(private val configList: List<ArcConfig>) :
    RecyclerView.Adapter<ArcConfigAdapter.ArcConfigViewHolder>() {

    class ArcConfigViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ringType: TextView = view.findViewById(R.id.ringType)
        val ringRadius: TextView = view.findViewById(R.id.ringRadius)
        val ringBulletCount: TextView = view.findViewById(R.id.ringBulletCount)
        val ringUpDown: TextView = view.findViewById(R.id.ringUpDown)
        val ringCloseFar: TextView = view.findViewById(R.id.ringCloseFar)
        val ringMinAngle: TextView = view.findViewById(R.id.ringMinAngle)
        val ringMaxAngle: TextView = view.findViewById(R.id.ringMaxAngle)
        val ringName: TextView = view.findViewById(R.id.ringName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArcConfigViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_arc_config, parent, false)
        return ArcConfigViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArcConfigViewHolder, position: Int) {
        val config = configList[position]
        holder.ringType.text = "Type: ${config.type}"
        holder.ringRadius.text = "Radius: ${config.radius}"
        holder.ringBulletCount.text = "Bullet Count: ${config.bulletCount}"
        holder.ringUpDown.text = "Up-Down: ${config.upDown}"
        holder.ringCloseFar.text = "Close-Far: ${config.closeFar}"
        holder.ringMinAngle.text = "Min Angle: ${config.minAngle}"
        holder.ringMaxAngle.text = "Max Angle: ${config.maxAngle}"
        holder.ringName.text = "Arc: ${position + 1}"
    }

    override fun getItemCount(): Int = configList.size
}

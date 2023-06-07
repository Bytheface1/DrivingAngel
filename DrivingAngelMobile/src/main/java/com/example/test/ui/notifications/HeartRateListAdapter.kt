package com.example.test.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test.R
import com.example.test.data.database.entities.HeartRateEntity
import java.text.SimpleDateFormat
import java.util.*

class HeartRateListAdapter : androidx.recyclerview.widget.ListAdapter<HeartRateEntity, HeartRateListAdapter.HeartRateViewHolder>(HEART_RATE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeartRateViewHolder {
        return HeartRateViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: HeartRateViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class HeartRateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val heartRateItemView: TextView = itemView.findViewById(R.id.tv_heart_rate)
        private val timestampItemView: TextView = itemView.findViewById(R.id.tv_timestamp)

        fun bind(heartRate: HeartRateEntity) {
            heartRateItemView.text = heartRate.heart_rate.toString()
            val sdf = SimpleDateFormat("MM/dd/yy hh:mm:ss", Locale.ENGLISH)
            val formatted: String = sdf.format(heartRate.timestamp)
            timestampItemView.text = formatted
        }

        companion object {
            fun create(parent: ViewGroup): HeartRateViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return HeartRateViewHolder(view)
            }
        }
    }

    companion object {
        private val HEART_RATE_COMPARATOR = object : DiffUtil.ItemCallback<HeartRateEntity>() {
            override fun areItemsTheSame(oldItem: HeartRateEntity, newItem: HeartRateEntity): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: HeartRateEntity, newItem: HeartRateEntity): Boolean {
                return oldItem.timestamp == newItem.timestamp
            }
        }
    }
}
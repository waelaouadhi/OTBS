package com.example.onetechbs.ui.training

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R
import com.example.onetechbs.db.TrainingResponse

class TrainingAdapter : ListAdapter<TrainingResponse, TrainingAdapter.TrainingViewHolder>(TrainingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_training, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = getItem(position)
        holder.bind(training)
    }

    class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.trainingName)
        private val dateTextView: TextView = itemView.findViewById(R.id.trainingDate)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.trainingDescription)

        fun bind(training: TrainingResponse) {
            nameTextView.text = training.title
            dateTextView.text = buildString {
                append(training.startDate)
                append(" - ")
                append(training.endDate)
            }
            descriptionTextView.text = training.description
        }
    }

    class TrainingDiffCallback : DiffUtil.ItemCallback<TrainingResponse>() {
        override fun areItemsTheSame(oldItem: TrainingResponse, newItem: TrainingResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrainingResponse, newItem: TrainingResponse): Boolean {
            return oldItem == newItem
        }
    }
}
package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.JobOfferResponseDTO
import com.google.android.material.button.MaterialButton

class JobAdapter(
    private val isHR: Boolean,
    private val onDeleteClick: (JobOfferResponseDTO) -> Unit,
    private val onUpdateClick: (JobOfferResponseDTO) -> Unit,
    private val onFinishClick: (JobOfferResponseDTO) -> Unit,
    private val onApplicantsClick: (JobOfferResponseDTO) -> Unit
) : ListAdapter<JobOfferResponseDTO, JobAdapter.JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job, isHR)
    }

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.jobTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.jobDescription)
        private val btnApplicants: MaterialButton = itemView.findViewById(R.id.btnApplicants)
        private val btnUpdate: MaterialButton = itemView.findViewById(R.id.btnUpdate)
        private val btnFinish: MaterialButton = itemView.findViewById(R.id.btnFinish)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)

        fun bind(job: JobOfferResponseDTO, isHR: Boolean) {
            titleText.text = "${job.title} - ${job.department}"
            descriptionText.text = job.description

            // Determine if job is finished
            val isFinished = job.status.equals("FINISHED", ignoreCase = true)

            // Apply view states
            if (isFinished) {
                // Dim text to indicate completion
                titleText.alpha = 0.4f
                descriptionText.alpha = 0.4f
            } else {
                titleText.alpha = 1f
                descriptionText.alpha = 1f
            }

            // Show/hide buttons considering HR role and finished state
            btnApplicants.visibility = if (isHR) View.VISIBLE else View.GONE
            btnUpdate.visibility = if (isHR && !isFinished) View.VISIBLE else View.GONE
            btnFinish.visibility = if (isHR && !isFinished) View.VISIBLE else View.GONE
            btnDelete.visibility = if (isHR) View.VISIBLE else View.GONE
            btnViewDetails.visibility = if (!isHR) View.VISIBLE else View.GONE

            // Set click listeners
            btnApplicants.setOnClickListener { onApplicantsClick(job) }
            btnUpdate.setOnClickListener { onUpdateClick(job) }
            btnFinish.setOnClickListener { onFinishClick(job) }
            btnDelete.setOnClickListener { onDeleteClick(job) }
            btnViewDetails.setOnClickListener {
                // Open JobOfferDetailsFragment
                val fragment = JobOfferDetailsFragment.newInstance(job)
                val transaction = (itemView.context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager?.beginTransaction()
                transaction?.replace(android.R.id.content, fragment)?.addToBackStack(null)?.commit()
            }
        }
    }

    class JobDiffCallback : DiffUtil.ItemCallback<JobOfferResponseDTO>() {
        override fun areItemsTheSame(oldItem: JobOfferResponseDTO, newItem: JobOfferResponseDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JobOfferResponseDTO, newItem: JobOfferResponseDTO): Boolean {
            return oldItem == newItem
        }
    }
} 
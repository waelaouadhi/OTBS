package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.JobOfferResponseDTO
import com.ramotion.foldingcell.FoldingCell

class JobAdapter(
    private val isHR: Boolean,
    private val onDeleteClick: (JobOfferResponseDTO) -> Unit,
    private val onUpdateClick: (JobOfferResponseDTO) -> Unit,
    private val onFinishClick: (JobOfferResponseDTO) -> Unit,
    private val onApplicantsClick: (JobOfferResponseDTO) -> Unit
) : ListAdapter<JobOfferResponseDTO, JobAdapter.JobViewHolder>(JobDiffCallback()) {

    // Track which item is currently expanded (-1 means none)
    private var expandedPosition = -1

    // Expand an item and collapse any previously expanded item
    private fun expandItem(position: Int) {
        val previousExpandedPosition = expandedPosition
        expandedPosition = position
        
        // Collapse previously expanded item
        if (previousExpandedPosition != -1 && previousExpandedPosition != position) {
            notifyItemChanged(previousExpandedPosition)
        }
        
        // Expand current item
        notifyItemChanged(position)
    }
    
    // Collapse an item
    private fun collapseItem(position: Int) {
        if (expandedPosition == position) {
            expandedPosition = -1
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_folding_cell, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job, isHR)
    }

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foldingCell: FoldingCell = itemView as FoldingCell
        private val jobTitleFolded: TextView = itemView.findViewById(R.id.jobTitleFolded)
        private val departmentFolded: TextView = itemView.findViewById(R.id.departmentFolded)
        private val btnViewDetailsFolded: MaterialButton = itemView.findViewById(R.id.btnViewDetailsFolded)

        private val jobTitleExpanded: TextView = itemView.findViewById(R.id.jobTitleExpanded)
        private val jobDescriptionExpanded: TextView = itemView.findViewById(R.id.jobDescriptionExpanded)
        private val departmentExpanded: TextView = itemView.findViewById(R.id.departmentExpanded)
        private val responsibilitiesExpanded: LinearLayout = itemView.findViewById(R.id.responsibilitiesExpanded)
        private val qualificationsExpanded: LinearLayout = itemView.findViewById(R.id.qualificationsExpanded)
        private val roleExpanded: TextView = itemView.findViewById(R.id.roleExpanded)
        private val btnApplyNow: MaterialButton = itemView.findViewById(R.id.btnApplyNow)
        private val btnSaveJob: MaterialButton = itemView.findViewById(R.id.btnSaveJob)
        private val btnViewApplicants: MaterialButton = itemView.findViewById(R.id.btnViewApplicants)
        private val btnEditJob: MaterialButton = itemView.findViewById(R.id.btnEditJob)
        private val btnCloseExpanded: MaterialButton = itemView.findViewById(R.id.btnCloseExpanded)

        fun bind(job: JobOfferResponseDTO, isHR: Boolean) {
            val shouldBeExpanded = adapterPosition == expandedPosition
            
            // Set the correct folding state based on accordion logic
            // Use post() to ensure view is measured before folding/unfolding
            itemView.post {
                if (shouldBeExpanded && !foldingCell.isUnfolded) {
                    foldingCell.unfold(false)
                } else if (!shouldBeExpanded && foldingCell.isUnfolded) {
                    foldingCell.fold(false)
                }
            }
            
            // Folded state
            jobTitleFolded.text = job.title
            departmentFolded.text = job.department

            // Expanded state
            jobTitleExpanded.text = job.title
            jobDescriptionExpanded.text = job.description
            departmentExpanded.text = job.department
            roleExpanded.text = job.role ?: ""

            // Populate responsibilities (as bullet points)
            responsibilitiesExpanded.removeAllViews()
            job.responsibilities?.split("\n", "•", "-")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { resp ->
                val bullet = TextView(itemView.context)
                bullet.text = "• $resp"
                bullet.setTextColor(android.graphics.Color.parseColor("#424242"))
                bullet.textSize = 14f
                responsibilitiesExpanded.addView(bullet)
            }
            // Populate qualifications (as bullet points or paragraph)
            qualificationsExpanded.removeAllViews()
            job.qualifications?.split("\n", "•", "-")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { qual ->
                val bullet = TextView(itemView.context)
                bullet.text = "• $qual"
                bullet.setTextColor(android.graphics.Color.parseColor("#424242"))
                bullet.textSize = 14f
                qualificationsExpanded.addView(bullet)
            }

            // Set up FoldingCell click listener
            btnViewDetailsFolded.setOnClickListener {
                expandItem(adapterPosition)
            }
            
            // Close button to fold back the expanded view
            btnCloseExpanded.setOnClickListener {
                collapseItem(adapterPosition)
            }
            
            // Also allow clicking anywhere on the folded view to expand
            foldingCell.setOnClickListener {
                if (!foldingCell.isUnfolded) {
                    expandItem(adapterPosition)
                }
            }

            // Set button visibility based on user role
            if (isHR) {
                // Show HR buttons, hide employee buttons
                btnApplyNow.visibility = View.GONE
                btnSaveJob.visibility = View.GONE
                btnViewApplicants.visibility = View.VISIBLE
                btnEditJob.visibility = View.VISIBLE
            } else {
                // Show employee buttons, hide HR buttons
                btnApplyNow.visibility = View.VISIBLE
                btnSaveJob.visibility = View.VISIBLE
                btnViewApplicants.visibility = View.GONE
                btnEditJob.visibility = View.GONE
            }
            
            // Employee action buttons
            btnApplyNow.setOnClickListener {
                // TODO: Implement apply logic
                android.widget.Toast.makeText(itemView.context, "Apply Now clicked", android.widget.Toast.LENGTH_SHORT).show()
            }
            btnSaveJob.setOnClickListener {
                // TODO: Implement save job logic
                android.widget.Toast.makeText(itemView.context, "Save Job clicked", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            // HR action buttons
            btnViewApplicants.setOnClickListener {
                onApplicantsClick(job)
            }
            btnEditJob.setOnClickListener {
                onUpdateClick(job)
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
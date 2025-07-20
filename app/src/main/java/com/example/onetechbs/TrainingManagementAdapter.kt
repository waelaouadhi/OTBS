package com.example.onetechbs

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R
import com.example.onetechbs.db.TrainingResponseDTO
import com.example.onetechbs.db.InvitationResponseDTO
import com.google.android.material.button.MaterialButton

class TrainingManagementAdapter(
    private val onViewDetailsClick: (Long) -> Unit,
    private val onDeleteClick: (Long) -> Unit
) : ListAdapter<TrainingResponseDTO, TrainingManagementAdapter.TrainingViewHolder>(TrainingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_management, parent, false)
        return TrainingViewHolder(view)

    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = getItem(position)
        Log.d("TrainingAdapter", "Binding training: ${training.title}")
        holder.bind(training)
    }

    inner class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.trainingTitleTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.trainingDateTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.trainingDescriptionTextView)
        private val participantsCountTextView: TextView = itemView.findViewById(R.id.participantsCountTextView)
        private val acceptedCountTextView: TextView = itemView.findViewById(R.id.acceptedCountTextView)
        private val participantsRecyclerView: RecyclerView = itemView.findViewById(R.id.participantsRecyclerView)

        private val deleteTrainingButton: MaterialButton = itemView.findViewById(R.id.deleteTrainingButton)

        @SuppressLint("SetTextI18n")
        fun bind(training: TrainingResponseDTO) {
            titleTextView.text = training.title
            dateTextView.text = "${training.startDate} - ${training.endDate}"
            descriptionTextView.text = training.description

            val invitations = training.invitations ?: emptyList()
            val totalParticipants = invitations.size
            val acceptedParticipants = invitations.count { it.status?.name == "CONFIRMED" }

            participantsCountTextView.text = "Total Participants: $totalParticipants"

            val acceptedNames = invitations
                .filter { it.status?.name == "CONFIRMED" }
                .mapNotNull { it.employeeName?.takeIf { name -> name.isNotBlank() } }
                .joinToString(separator = "\n")

            acceptedCountTextView.text = if (acceptedNames.isNotEmpty()) {
                "Accepted:\n$acceptedNames"
            } else {
                "Accepted: 0"
            }

            setupParticipantsList(invitations)

            deleteTrainingButton.setOnClickListener {
                onDeleteClick(training.id)
            }
        }

        private fun setupParticipantsList(invitations: List<InvitationResponseDTO>) {
            val participantsAdapter = ParticipantsAdapter()
            participantsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = participantsAdapter
            }
            participantsAdapter.submitList(invitations)
        }
    }

    class TrainingDiffCallback : DiffUtil.ItemCallback<TrainingResponseDTO>() {
        override fun areItemsTheSame(oldItem: TrainingResponseDTO, newItem: TrainingResponseDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrainingResponseDTO, newItem: TrainingResponseDTO): Boolean {
            return oldItem == newItem
        }
    }
}

class ParticipantsAdapter : ListAdapter<InvitationResponseDTO, ParticipantsAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = getItem(position)
        holder.bind(participant)
    }

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        @SuppressLint("SetTextI18n")
        fun bind(participant: InvitationResponseDTO) {
            val name = participant.employeeName.ifBlank { "Unknown" }
            textView.text = "$name (${participant.status})"
        }
    }

    class ParticipantDiffCallback : DiffUtil.ItemCallback<InvitationResponseDTO>() {
        override fun areItemsTheSame(oldItem: InvitationResponseDTO, newItem: InvitationResponseDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InvitationResponseDTO, newItem: InvitationResponseDTO): Boolean {
            return oldItem == newItem
        }
    }
}
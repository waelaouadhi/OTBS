package com.example.onetechbs

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R
import com.example.onetechbs.db.TrainingResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrainingAdapter(private val userId: String) : ListAdapter<TrainingResponseDTO, TrainingAdapter.TrainingViewHolder>(TrainingDiffCallback()) {

    private var allTrainings: List<TrainingResponseDTO> = emptyList()
    private var currentFilter: TrainingFilter = TrainingFilter.ALL

    enum class TrainingFilter {
        ALL, AVAILABLE, ACCEPTED, REJECTED
    }

    fun updateTrainings(trainings: List<TrainingResponseDTO>) {
        allTrainings = trainings
        // Apply the current filter, which now considers userId for relevant filter types
        filterTrainings(currentFilter)
    }

    fun filterTrainings(filter: TrainingFilter) {
        currentFilter = filter
        val filteredList = when (filter) {
            TrainingFilter.ALL -> allTrainings // Show all trainings relevant to the user (pre-filtered by fragment if employee)
            TrainingFilter.AVAILABLE -> allTrainings.filter { training ->
                // Show trainings where the current user has a PENDING invitation
                training.invitations.any { it.employeeId == userId && it.status == "PENDING" }
            }
            TrainingFilter.ACCEPTED -> allTrainings.filter { training ->
                // Show trainings where the current user has a CONFIRMED invitation
                training.invitations.any { it.employeeId == userId && it.status == "CONFIRMED" }
            }
            TrainingFilter.REJECTED -> allTrainings.filter { training ->
                // Show trainings where the current user has a REJECTED invitation
                training.invitations.any { it.employeeId == userId && it.status == "REJECTED" }
            }
        }
        submitList(filteredList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training, parent, false)
        return TrainingViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val training = getItem(position)
        holder.bind(training)
    }

    inner class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.trainingName)
        private val dateTextView: TextView = itemView.findViewById(R.id.trainingDate)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.trainingDescription)
        private val acceptButton: MaterialButton = itemView.findViewById(R.id.acceptButton)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(training: TrainingResponseDTO) {
            nameTextView.text = training.title
            dateTextView.text = buildString {
                append(training.startDate)
                append(" - ")
                append(training.endDate)
            }
            descriptionTextView.text = training.description

            // Find the invitation specifically for the current logged-in user
            val currentUserInvitation = training.invitations.find { it.employeeId == userId }
            Log.d("TrainingAdapter", "Current User ID: $userId, Found invitation for user: ${currentUserInvitation?.id} with status: ${currentUserInvitation?.status}")

            if (currentUserInvitation != null) {
                acceptButton.visibility = View.VISIBLE
                when (currentUserInvitation.status) {
                    "PENDING" -> {
                        acceptButton.isEnabled = true
                        acceptButton.text = "Accept"
                        acceptButton.setOnClickListener {
                            confirmInvitation(currentUserInvitation.id)
                        }
                    }
                    "CONFIRMED" -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Already Accepted"
                    }
                    "REJECTED" -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Rejected"
                    }
                    else -> {
                        // Handle other statuses or hide button if not actionable
                        acceptButton.isEnabled = false
                        acceptButton.text = "Status: ${currentUserInvitation.status}" // Or "Not Available"
                    }
                }
            } else {
                // No invitation for this user for this training, hide the button
                acceptButton.visibility = View.GONE
                Log.d("TrainingAdapter", "No invitation found for user $userId in training ${training.id}")
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun confirmInvitation(invitationId: Long) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = RetrofitClient.getAuthToken()
                    if (token.isBlank()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(itemView.context, "Authentication required", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    Log.d("TrainingAdapter", "Making API call to confirm invitation ID: $invitationId")
                    val response = RetrofitClient.trainingService.confirmInvitation("Bearer $token", invitationId)
                    Log.d("TrainingAdapter", "Response code: ${response.code()}, body: ${response.body()}, error: ${response.errorBody()?.string()}")
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(itemView.context, "Training invitation accepted successfully!", Toast.LENGTH_SHORT).show()
                            // Update the UI to reflect the change
                            acceptButton.isEnabled = false
                            acceptButton.text = "Already Accepted"
                            // Refresh the list to update the filter
                            filterTrainings(currentFilter)
                        } else {
                            val errorMessage = when (response.code()) {
                                400 -> {
                                    // Update UI to show invitation is no longer available
                                    acceptButton.isEnabled = false
                                    acceptButton.text = "Not Available"
                                    "This invitation is no longer available for confirmation."
                                }
                                401 -> "Authentication failed. Please login again."
                                403 -> "You don't have permission to accept this invitation."
                                else -> "Failed to accept invitation: ${response.errorBody()?.string()}"
                            }
                            Toast.makeText(itemView.context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TrainingAdapter", "Error confirming invitation", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(itemView.context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
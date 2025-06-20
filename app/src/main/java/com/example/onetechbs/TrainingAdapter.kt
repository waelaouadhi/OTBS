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

import com.example.onetechbs.db.TrainingResponseDTO
import com.example.onetechbs.db.EStatus
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrainingAdapter(private val userId: String) :
    ListAdapter<TrainingResponseDTO, TrainingAdapter.TrainingViewHolder>(TrainingDiffCallback()) {

    private var allTrainings: List<TrainingResponseDTO> = emptyList()
    private var currentFilter: TrainingFilter = TrainingFilter.ALL

    enum class TrainingFilter {
        ALL, AVAILABLE, ACCEPTED, REJECTED
    }

    fun updateTrainings(trainings: List<TrainingResponseDTO>) {
        allTrainings = trainings
        filterTrainings(currentFilter)
    }

    fun filterTrainings(filter: TrainingFilter) {
        currentFilter = filter
        val filteredList = when (filter) {
            TrainingFilter.ALL -> allTrainings
            TrainingFilter.AVAILABLE -> allTrainings.filter {
                it.invitations.any { inv -> inv.employeeId == userId && inv.status == EStatus.PENDING }
            }
            TrainingFilter.ACCEPTED -> allTrainings.filter {
                it.invitations.any { inv -> inv.employeeId == userId && inv.status == EStatus.CONFIRMED }
            }
            TrainingFilter.REJECTED -> allTrainings.filter {
                it.invitations.any { inv -> inv.employeeId == userId && inv.status == EStatus.REFUSÉE }
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
            dateTextView.text = "${training.startDate} - ${training.endDate}"
            descriptionTextView.text = training.description

            val currentUserInvitation = training.invitations.find { it.employeeId == userId }

            Log.d(
                "TrainingAdapter",
                "User: $userId, Training ID: ${training.id}, Invitation: ${currentUserInvitation?.id}, Status: ${currentUserInvitation?.status}"
            )

            if (currentUserInvitation != null) {
                acceptButton.visibility = View.VISIBLE
                when (currentUserInvitation.status) {
                    EStatus.PENDING -> {
                        acceptButton.isEnabled = true
                        acceptButton.text = "Accept"
                        acceptButton.setOnClickListener {
                            // Pass training ID instead of invitation ID
                            confirmInvitation(currentUserInvitation.id)
                        }
                    }
                    EStatus.CONFIRMED -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Already Accepted"
                    }
                    EStatus.REFUSÉE -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Rejected"
                    }
                    else -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Status: ${currentUserInvitation.status}"
                    }
                }
            } else {
                acceptButton.visibility = View.GONE
            }
        }




        @RequiresApi(Build.VERSION_CODES.O)
        private fun confirmInvitation(invitationId: Long) {
            CoroutineScope(Dispatchers.IO).launch {
                val context = itemView.context
                val token = SharedPreferencesManager.getInstance(context).getAuthToken()

                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Authentication token missing. Please log in again.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    acceptButton.isEnabled = false
                    acceptButton.text = "Processing..."
                }

                try {
                    Log.d("TrainingAdapter", "Calling confirmInvitation with invitationId: $invitationId")

                    val response = RetrofitClient.trainingService.confirmInvitation(
                        "Bearer $token",
                        invitationId
                    )

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Training invitation accepted successfully!", Toast.LENGTH_SHORT).show()
                            acceptButton.text = "Already Accepted"

                            // Update the invitation status in the current item
                            val currentPosition = adapterPosition
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                val currentTraining = getItem(currentPosition)
                                currentTraining.invitations.find { it.id == invitationId }?.status = EStatus.CONFIRMED
                                filterTrainings(currentFilter)
                            }
                        } else {
                            acceptButton.isEnabled = true
                            acceptButton.text = "Accept"

                            val errorMessage = when (response.code()) {
                                400 -> {
                                    Log.e("TrainingAdapter", "Bad request: ${response.errorBody()?.string()}")
                                    "This invitation is no longer available for confirmation."
                                }
                                401 -> "Authentication failed. Please login again."
                                403 -> "You don't have permission to accept this invitation."
                                404 -> "Training or invitation not found."
                                else -> {
                                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                    Log.e("TrainingAdapter", "HTTP ${response.code()}: $errorBody")
                                    "Failed to accept invitation. Please try again."
                                }
                            }
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TrainingAdapter", "Error confirming invitation", e)
                    withContext(Dispatchers.Main) {
                        acceptButton.isEnabled = true
                        acceptButton.text = "Accept"
                        Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }}

    class TrainingDiffCallback : DiffUtil.ItemCallback<TrainingResponseDTO>() {
        override fun areItemsTheSame(oldItem: TrainingResponseDTO, newItem: TrainingResponseDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TrainingResponseDTO, newItem: TrainingResponseDTO): Boolean {
            return oldItem == newItem
        }
    }
}
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
                it.invitations?.any { inv -> inv.employeeId == userId && inv.status == EStatus.PENDING } == true
            }
            TrainingFilter.ACCEPTED -> allTrainings.filter {
                it.invitations?.any { inv -> inv.employeeId == userId && inv.status == EStatus.APPROVED } == true
            }
            TrainingFilter.REJECTED -> allTrainings.filter {
                it.invitations?.any { inv -> inv.employeeId == userId && inv.status == EStatus.REJECTED } == true
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

            val currentUserInvitation = training.invitations?.find { it.employeeId == userId }

            Log.d("TrainingAdapter", "User: $userId, Training ID: ${training.id}, Invitation: ${currentUserInvitation?.id}, Status: ${currentUserInvitation?.status}")
            Log.d("TrainingAdapter", "All invitation employeeIds for training ${training.id}: ${training.invitations?.map { it.employeeId }}")
            training.invitations?.forEach {
                Log.d("TrainingAdapter", "Invitation: id=${it.id}, employeeId='${it.employeeId}', status=${it.status}")
            }

            acceptButton.visibility = View.VISIBLE
            if (currentUserInvitation == null) {
                acceptButton.isEnabled = true
                acceptButton.text = "Accept"
                acceptButton.setOnClickListener {
                    Toast.makeText(itemView.context, "You do not have an invitation for this training. Please contact your admin or wait to be invited.", Toast.LENGTH_SHORT).show()
                }
            } else {
                when (currentUserInvitation.status) {
                    EStatus.PENDING -> {
                        acceptButton.isEnabled = true
                        acceptButton.text = "Accept"
                        acceptButton.setOnClickListener {
                            confirmInvitation(currentUserInvitation.id)
                        }
                    }
                    EStatus.APPROVED -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Already Accepted"
                    }
                    EStatus.REJECTED -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Rejected"
                    }
                    else -> {
                        acceptButton.isEnabled = false
                        acceptButton.text = "Status: ${currentUserInvitation.status}"
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun confirmInvitation(invitationId: Long) {
            Log.d("TrainingAdapter", "[DEBUG] confirmInvitation CALLED with invitationId=$invitationId")
            CoroutineScope(Dispatchers.IO).launch {
                val context = itemView.context
                val token = SharedPreferencesManager.getInstance(context).getAuthToken()
                Log.d("TrainingAdapter", "[DEBUG] Retrieved token: ${token?.take(8)}... (length=${token?.length})")

                if (token.isNullOrBlank()) {
                    Log.e("TrainingAdapter", "[DEBUG] Token is null or blank!")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Authentication token missing. Please log in again.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    Log.d("TrainingAdapter", "[DEBUG] Disabling acceptButton and setting text to Processing...")
                    acceptButton.isEnabled = false
                    acceptButton.text = "Processing..."
                }

                try {
                    Log.d("TrainingAdapter", "[DEBUG] About to call RetrofitClient.trainingService.confirmInvitation with id=$invitationId")
                    val response = RetrofitClient.trainingService.confirmInvitation(
                        "Bearer $token",
                        invitationId
                    )
                    Log.d("TrainingAdapter", "[DEBUG] Retrofit response: isSuccessful=${response.isSuccessful}, code=${response.code()}, message=${response.message()}")

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d("TrainingAdapter", "[DEBUG] Invitation confirmed successfully for invitationId=$invitationId")
                            Toast.makeText(context, "Training invitation accepted successfully!", Toast.LENGTH_SHORT).show()
                            acceptButton.text = "Already Accepted"
                            // Update the invitation status in the current item
                            val currentPosition = adapterPosition
                            Log.d("TrainingAdapter", "[DEBUG] Adapter position: $currentPosition")
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                val currentTraining = getItem(currentPosition)
                                Log.d("TrainingAdapter", "[DEBUG] Current training: id=${currentTraining.id}, title=${currentTraining.title}")
                                val invitation = currentTraining.invitations?.find { it.id == invitationId }
                                Log.d("TrainingAdapter", "[DEBUG] Current invitation before update: $invitation")
                                if (invitation != null) {
                                    invitation.status = EStatus.APPROVED
                                    Log.d("TrainingAdapter", "[DEBUG] Invitation status updated to APPROVED")
                                } else {
                                    Log.e("TrainingAdapter", "[DEBUG] Could not find invitation with id=$invitationId in currentTraining.invitations")
                                }
                                filterTrainings(currentFilter)
                            } else {
                                Log.e("TrainingAdapter", "[DEBUG] Invalid adapter position: $currentPosition")
                            }
                        } else {
                            Log.e("TrainingAdapter", "[DEBUG] Response NOT successful. code=${response.code()} body=${response.errorBody()?.string()} ")
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
                    Log.e("TrainingAdapter", "[DEBUG] Exception thrown in confirmInvitation", e)
                    withContext(Dispatchers.Main) {
                        acceptButton.isEnabled = true
                        acceptButton.text = "Accept"
                        Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                Log.d("TrainingAdapter", "[DEBUG] confirmInvitation END for invitationId=$invitationId")
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
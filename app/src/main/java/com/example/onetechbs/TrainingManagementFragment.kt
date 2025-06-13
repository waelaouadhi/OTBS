package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentTrainingManagementBinding
import com.example.onetechbs.db.TrainingResponseDTO
import com.example.onetechbs.network.RetrofitClient
import kotlinx.coroutines.launch

class TrainingManagementFragment : Fragment() {
    private var _binding: FragmentTrainingManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TrainingManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("TrainingFragment", "onCreateView called")
        _binding = FragmentTrainingManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TrainingFragment", "onViewCreated called")

        try {
            setupRecyclerView()
            viewLifecycleOwner.lifecycleScope.launch {
                loadTrainings()
            }
        } catch (e: Exception) {
            Log.e("TrainingFragment", "Error in onViewCreated", e)
            showError("Error initializing view: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        Log.d("TrainingFragment", "Setting up RecyclerView")
        try {
            adapter = TrainingManagementAdapter(
                onViewDetailsClick = { trainingId ->
                    Log.d("TrainingFragment", "Training details clicked for ID: $trainingId")
                    loadTrainingDetails(trainingId)
                },
                onDeleteClick = { trainingId ->
                    showDeleteConfirmationDialog(trainingId)
                }
            )

            binding.trainingsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TrainingManagementFragment.adapter
                setHasFixedSize(true)
                visibility = View.VISIBLE
            }

            Log.d("TrainingFragment", "RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e("TrainingFragment", "Error setting up RecyclerView", e)
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadTrainings() {
        Log.d("TrainingFragment", "Starting to load trainings")
        showLoading(true)

        val token = RetrofitClient.getAuthToken()
        Log.d("TrainingFragment", "Retrieved token: ${token.take(10)}...")

        if (token.isBlank()) {
            Log.e("TrainingFragment", "Token is blank")
            showError("Authentication required")
            showLoading(false)
            return
        }

        try {
            val response = RetrofitClient.trainingService.getTrainings("Bearer $token")
            showLoading(false)
            Log.d("TrainingFragment", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                val trainings = response.body()
                Log.d("TrainingFragment", "Received ${trainings?.size ?: 0} trainings")

                if (trainings != null && trainings.isNotEmpty()) {
                    updateStatistics(trainings)
                    adapter.submitList(trainings)
                    binding.errorMessage.visibility = View.GONE
                    binding.trainingsRecyclerView.visibility = View.VISIBLE
                    Log.d("TrainingFragment", "Successfully loaded and displayed trainings")
                } else {
                    Log.d("TrainingFragment", "No trainings found in response")
                    showError("No trainings found")
                    binding.trainingsRecyclerView.visibility = View.GONE
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("TrainingFragment", "Error response: $errorBody")
                when (response.code()) {
                    401 -> showError("Authentication failed. Please login again.")
                    403 -> showError("You don't have permission to view trainings.")
                    else -> showError("Failed to load trainings: $errorBody")
                }
                binding.trainingsRecyclerView.visibility = View.GONE
            }
        } catch (e: Exception) {
            showLoading(false)
            Log.e("TrainingFragment", "API call failed", e)
            showError("Error: ${e.message}")
            binding.trainingsRecyclerView.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTrainingById(trainingId: Long) {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = RetrofitClient.getAuthToken()
                if (token.isBlank()) {
                    showError("Authentication required")
                    return@launch
                }

                val response = RetrofitClient.trainingService.getTrainingById("Bearer $token", trainingId)
                if (response.isSuccessful) {
                    val training = response.body()
                    if (training != null) {
                        val trainingList = listOf(training)
                        updateStatistics(trainingList)
                        adapter.submitList(trainingList)
                        binding.errorMessage.visibility = View.GONE
                    } else {
                        showError("Training not found")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    when (response.code()) {
                        401 -> showError("Authentication failed. Please login again.")
                        403 -> showError("You don't have permission to view this training.")
                        404 -> showError("Training not found.")
                        else -> showError("Failed to load training: $errorBody")
                    }
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTrainingDetails(trainingId: Long) {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = RetrofitClient.getAuthToken()
                if (token.isBlank()) {
                    showError("Authentication required")
                    return@launch
                }

                val response = RetrofitClient.trainingService.getTrainingById("Bearer $token", trainingId)
                if (response.isSuccessful) {
                    val training = response.body()
                    if (training != null) {
                        showTrainingDetails(training)
                    } else {
                        showError("Training not found")
                    }
                } else {
                    showError("Failed to load training details: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDeleteConfirmationDialog(trainingId: Long) {
        val context = requireContext()
        android.app.AlertDialog.Builder(context)
            .setTitle("Delete Training")
            .setMessage("Are you sure you want to delete this training?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTraining(trainingId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteTraining(trainingId: Long) {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = RetrofitClient.getAuthToken()
                if (token.isBlank()) {
                    showError("Authentication required")
                    return@launch
                }
                val apiService = RetrofitClient.trainingService2
                val response = apiService.deleteTraining(trainingId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Training deleted successfully", Toast.LENGTH_SHORT).show()
                    loadTrainings()
                } else {
                    showError("Failed to delete training: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                showError("Error deleting training: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateStatistics(trainings: List<TrainingResponseDTO>) {
        try {
            val totalTrainings = trainings.size
            val activeTrainings = trainings.count { training ->
                training.invitations.any { it.status.name == "PENDING" }
            }

            binding.totalTrainingsTextView.text = "Total Trainings: $totalTrainings"
            binding.activeTrainingsTextView.text = "Active Trainings: $activeTrainings"
            Log.d("TrainingFragment", "Updated statistics - Total: $totalTrainings, Active: $activeTrainings")
        } catch (e: Exception) {
            Log.e("TrainingFragment", "Error updating statistics", e)
        }
    }

    private fun showTrainingDetails(training: TrainingResponseDTO) {
        val acceptedCount = training.invitations.count { it.status.name == "ACCEPTED" }
        val pendingCount = training.invitations.count { it.status.name == "PENDING" }

        val message = """
            Title: ${training.title}
            Department: ${training.department}
            Date: ${training.startDate} - ${training.endDate}
            Created by: ${training.createdBy}
            Created at: ${training.createdAt}
            
            Total Invitations: ${training.invitations.size}
            Accepted: $acceptedCount
            Pending: $pendingCount
        """.trimIndent()

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.trainingsRecyclerView.visibility = View.GONE
            binding.errorMessage.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.errorMessage.text = message
        binding.errorMessage.visibility = View.VISIBLE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
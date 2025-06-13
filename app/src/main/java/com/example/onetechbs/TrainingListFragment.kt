package com.example.onetechbs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.TrainingResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.TrainingAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class TrainingListFragment : Fragment() {

    private lateinit var trainingRecyclerView: RecyclerView
    private lateinit var trainingAdapter: TrainingAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var filterChipGroup: ChipGroup

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_training_list, container, false)

        // Initialize the RecyclerView
        trainingRecyclerView = binding.findViewById(R.id.trainingRecyclerView)
        trainingRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the Adapter
        // Retrieve userId to pass to the adapter
        val prefsOnCreate = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userIdOnCreate = prefsOnCreate.getString("userId", "") ?: ""
        trainingAdapter = TrainingAdapter(userIdOnCreate) // Pass userId to adapter
        trainingRecyclerView.adapter = trainingAdapter

        // Initialize the ProgressBar
        progressBar = binding.findViewById(R.id.progressBar)

        // Initialize the Filter Chip Group
        filterChipGroup = binding.findViewById(R.id.filterChipGroup)

        // Initial setup of chips is done in onViewCreated
        // Data fetching will be handled in onResume

        return binding
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Fetch training data every time the fragment is resumed to ensure freshness
        fetchTrainings()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchTrainings() {
        progressBar.visibility = View.VISIBLE

        // Get user role from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userRole = prefs.getString("role", "employee")
        // Retrieve userId as String, default to empty string if not found
        val userId = prefs.getString("userId", "")

        RetrofitClient.trainingService.getAllTrainings("Bearer ${RetrofitClient.getAuthToken()}").enqueue(object :
            Callback<List<TrainingResponseDTO>> {
            override fun onResponse(
                call: Call<List<TrainingResponseDTO>>,
                response: Response<List<TrainingResponseDTO>>
            ) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val trainings = response.body()
                    if (trainings != null) {
                        // Filter trainings based on role
                        // Now, both employees and managers will see all trainings initially.
                        // The adapter will handle the display logic based on whether an invitation exists.
                        val trainingsForAdapter: List<TrainingResponseDTO> = trainings

                        if (trainingsForAdapter.isNotEmpty()) {
                            trainingAdapter.updateTrainings(trainingsForAdapter)
                        } else {
                            showToast("No trainings available.")
                        }
                    } else {
                        showToast("No trainings available.")
                    }
                } else {
                    showToast("Failed to load trainings: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<TrainingResponseDTO>>, t: Throwable) {
                progressBar.visibility = View.GONE
                showToast("Error fetching trainings: ${t.localizedMessage}")
            }
        })
    }
    private fun showToast(message: String) {
        // Show a toast on the main thread
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilterChips()
    }

    private fun setupFilterChips() {
        filterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val filter = when (checkedId) {
                R.id.chipAll -> TrainingAdapter.TrainingFilter.ALL
                R.id.chipAvailable -> TrainingAdapter.TrainingFilter.AVAILABLE
                R.id.chipAccepted -> TrainingAdapter.TrainingFilter.ACCEPTED
                R.id.chipRejected -> TrainingAdapter.TrainingFilter.REJECTED
                else -> TrainingAdapter.TrainingFilter.ALL
            }
            trainingAdapter.filterTrainings(filter)
        }
    }
}
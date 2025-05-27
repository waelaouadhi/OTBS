package com.example.onetechbs.ui.training

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R
import com.example.onetechbs.db.TrainingResponse
import com.example.onetechbs.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TrainingListFragment : Fragment() {

    private lateinit var trainingRecyclerView: RecyclerView
    private lateinit var trainingAdapter: TrainingAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_training_list, container, false)

        // Initialize the RecyclerView
        trainingRecyclerView = binding.findViewById(R.id.trainingLayout)
        trainingRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the Adapter
        trainingAdapter = TrainingAdapter()
        trainingRecyclerView.adapter = trainingAdapter

        // Initialize the ProgressBar
        progressBar = binding.findViewById(R.id.progressBar)

        // Fetch training data
        fetchTrainings()

        return binding
    }

    private fun fetchTrainings() {
        // Show ProgressBar when fetching data
        progressBar.visibility = View.VISIBLE

        RetrofitClient.trainingService.getAllTrainings().enqueue(object : Callback<List<TrainingResponse>> {
            override fun onResponse(
                call: Call<List<TrainingResponse>>,
                response: Response<List<TrainingResponse>>
            ) {
                // Hide ProgressBar after data is loaded
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val trainings = response.body()
                    if (trainings != null) {
                        // Update the UI with the training data
                        trainingAdapter.submitList(trainings)
                    } else {
                        showToast("No trainings available.")
                    }
                } else {
                    showToast("Failed to load trainings: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<TrainingResponse>>, t: Throwable) {
                // Hide ProgressBar on failure
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
}
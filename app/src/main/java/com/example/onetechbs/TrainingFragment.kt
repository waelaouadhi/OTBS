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
import com.example.onetechbs.databinding.FragmentTrainingBinding
import com.example.onetechbs.db.NotificationRequest
import com.example.onetechbs.db.TrainingRequest
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)

        setupUI()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUI() {
        // Setup department dropdown
        val departments = listOf("HR", "Engineering", "Marketing", "Sales", "Finance", "Operations")
        val deptAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, departments)
        binding.departmentEditText.setAdapter(deptAdapter)
        // Set up toolbar
        val toolbar = binding.root.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            // Navigate back to home fragment
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.selectDateRangeButton.setOnClickListener {
            showDateRangePicker()
        }

        binding.submitTrainingButton.setOnClickListener {
            submitTraining()
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Training Date Range")
            .build()

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")

        picker.addOnPositiveButtonClickListener { selection ->
            selection?.let {
                val startMillis = it.first ?: return@let
                val endMillis = it.second ?: return@let
                selectedStartDate = dateFormat.format(Date(startMillis))
                selectedEndDate = dateFormat.format(Date(endMillis))
                Toast.makeText(
                    requireContext(),
                    "Selected: $selectedStartDate to $selectedEndDate",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitTraining() {
        val title = binding.trainingNameEditText.text.toString().trim()
        val description = binding.trainingDescriptionEditText.text.toString().trim()
        val department = binding.departmentEditText.text.toString().trim()
        val prefsManager = SharedPreferencesManager.getInstance(requireContext())
        val token = prefsManager.getAuthToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication token missing! Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isEmpty() || description.isEmpty() || department.isEmpty() ||
            selectedStartDate == null || selectedEndDate == null) {
            val msg = buildString {
                if (title.isEmpty()) append("Title, ")
                if (description.isEmpty()) append("Description, ")
                if (department.isEmpty()) append("Department, ")
                if (selectedStartDate==null || selectedEndDate==null) append("Date range, ")
            }.removeSuffix(", ")
            Toast.makeText(requireContext(), "Please complete: $msg", Toast.LENGTH_LONG).show()
            return
        }

        val request = TrainingRequest(
            title = title,
            description = description,
            department = department,
            startDate = selectedStartDate!!,
            endDate = selectedEndDate!!,
            createdBy = "Manager"
        )

        RetrofitClient.trainingService.createTraining(
            "Bearer $token", // Pass the token as the Authorization header
            request
        ).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Training Created ✅", Toast.LENGTH_SHORT).show()
                    clearInputs()
                    createNotificationForTraining(title, description)
                } else {
                    Toast.makeText(requireContext(), "Failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }@RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationForTraining(title: String, message: String,
                                              recipient: String = "All Employees", sender: String = "Manager", type: String = "TRAINING", actionUrl: String = "http://example.com/training/details", read: Boolean = true
    ) {
        lifecycleScope.launch {
            try {
                val notification = NotificationRequest(
                    read = read,
                    recipient ="A new training session has been created: $recipient", // Set recipient dynamically
                    sender = "Manager: $sender", // Set sender dynamically
                    type = "TRAINING",
                    message = "A new training session has been created: $message",
                    actionUrl = "http://example.com/training/details: $actionUrl",  // URL pointing to the training details
                    title = title
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.notificationService.createNotification(notification)
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Notification Created ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to create notification: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                        Log.e("TrainingFragment", "Failed to create notification: ${response.errorBody()?.string()}, ${response.code()}")
                        Log.i("TrainingFragment", "Response: $response")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearInputs() {
        binding.trainingNameEditText.text?.clear()
        binding.trainingDescriptionEditText.text?.clear()
        binding.departmentEditText.text?.clear()
        selectedStartDate = null
        selectedEndDate = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
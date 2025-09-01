package com.example.onetechbs

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentEnrollmentsListBinding
import com.example.onetechbs.db.TrainingRequestResponseDTO
import com.example.onetechbs.db.TrainingRequestReviewRequestDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch

class EnrollmentsListFragment : Fragment() {
    private var _binding: FragmentEnrollmentsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EnrollmentsAdapter
    private var userRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnrollmentsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarEnrollmentsList.setNavigationOnClickListener { requireActivity().onBackPressed() }
        userRole = SharedPreferencesManager.getInstance(requireContext()).getUserRole()
        setupRecyclerView()
        binding.swipeRefreshEnrollments.setOnRefreshListener { fetchEnrollments() }
        fetchEnrollments()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewEnrollments.layoutManager = LinearLayoutManager(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchEnrollments() {
        binding.progressBarEnrollments.visibility = View.VISIBLE
        binding.textEmptyEnrollments.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
                val response = RetrofitClient.trainingService2.getAllTrainingRequests("Bearer $token")
                if (response.isSuccessful) {
                    val enrollments = (response.body() ?: emptyList())
                        .sortedWith(compareByDescending<TrainingRequestResponseDTO> { it.requestDate }
                            .thenByDescending { it.id })
                    if (enrollments.isEmpty()) {
                        binding.textEmptyEnrollments.visibility = View.VISIBLE
                    }
                    adapter = EnrollmentsAdapter(enrollments, userRole,
                        onApprove = { enrollment -> approveEnrollment(enrollment) },
                        onReject = { enrollment -> rejectEnrollment(enrollment) }
                    )
                    binding.recyclerViewEnrollments.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Failed to load enrollments", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace() // ← This logs full stack trace to console
                Log.e("PropositionListError", "Exception: ${e.message}")
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBarEnrollments.visibility = View.GONE
                binding.swipeRefreshEnrollments.isRefreshing = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun approveEnrollment(enrollment: TrainingRequestResponseDTO) {
        binding.progressBarEnrollments.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
                val response = RetrofitClient.trainingService2.approveEnrollment(enrollment.id, "Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Enrollment approved!", Toast.LENGTH_SHORT).show()
                    fetchEnrollments()
                } else {
                    Toast.makeText(requireContext(), "Failed to approve: ${response.errorBody()?.string() ?: response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBarEnrollments.visibility = View.GONE
            }
        }
    }

    private fun rejectEnrollment(enrollment: TrainingRequestResponseDTO) {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Enrollment")
            .setMessage("Enter rejection reason:")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(requireContext(), "Rejection reason required", Toast.LENGTH_SHORT).show()
                } else {
                    performRejectEnrollment(enrollment, reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRejectEnrollment(enrollment: TrainingRequestResponseDTO, reason: String) {
        binding.progressBarEnrollments.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
                val review = TrainingRequestReviewRequestDTO(reason)
                val response = RetrofitClient.trainingService2.rejectEnrollment(enrollment.id, "Bearer $token", review)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Enrollment rejected!", Toast.LENGTH_SHORT).show()
                    fetchEnrollments()
                } else {
                    Toast.makeText(requireContext(), "Failed to reject: ${response.errorBody()?.string() ?: response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBarEnrollments.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.onetechbs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentDoctorListBinding
import com.example.onetechbs.db.MedicalVisitResponse
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class DoctorListFragment : Fragment(R.layout.fragment_doctor_list) {

    private var _binding: FragmentDoctorListBinding? = null
    private val binding get() = _binding!!

    private lateinit var doctorVisitAdapter: DoctorVisitAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDoctorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupRetryButton()
        fetchDoctorVisits()
    }

    private fun setupRecyclerView() {
        doctorVisitAdapter = DoctorVisitAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = doctorVisitAdapter
        }
    }

    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            fetchDoctorVisits()
        }
    }

    private fun fetchDoctorVisits() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.medService.getDoctorVisits()
                Log.d("DoctorListFragment", "Response Code: ${response.code()}")
                Log.d("DoctorListFragment", "Response Body: ${response.body()}")

                if (response.isSuccessful) {
                    response.body()?.let {
                        doctorVisitAdapter.submitList(it)
                        showLoading(false)
                        showError(false)
                    } ?: run {
                        showLoading(false)
                        showError(true, "No data available")
                    }
                } else {
                    showLoading(false)
                    showError(true, "Failed to load doctor visits: ${response.code()} ${response.message()}")
                }
            } catch (e: HttpException) {
                Log.e("DoctorListFragment", "HttpException: ${e.message()}")
                showLoading(false)
                showError(true, "Server error: ${e.message()}")
            } catch (e: IOException) {
                Log.e("DoctorListFragment", "IOException: ${e.message}")
                showLoading(false)
                showError(true, "Check your internet connection.")
            } catch (e: Exception) {
                Log.e("DoctorListFragment", "Unexpected Exception: ${e.message}")
                showLoading(false)
                showError(true, "An unexpected error occurred: ${e.message}")
            }
        }
    }
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.retryButton.visibility = if (isLoading) View.GONE else binding.retryButton.visibility
    }

    private fun showError(show: Boolean, message: String = "") {
        if (show) {
            binding.errorTextView.visibility = View.VISIBLE
            binding.errorTextView.text = "Error: $message"
            binding.retryButton.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.errorTextView.visibility = View.GONE
            binding.retryButton.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
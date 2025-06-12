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
import com.example.onetechbs.adapter.DoctorManagementAdapter
import com.example.onetechbs.databinding.FragmentDoctorManagementBinding
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch
import retrofit2.HttpException

class DoctorManagementFragment : Fragment() {

    private var _binding: FragmentDoctorManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DoctorManagementAdapter
    private val TAG = "DoctorManagementFrag"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        initializeToken()
        loadVisits()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        adapter = DoctorManagementAdapter(onDeleteClicked = { visit ->
            deleteVisit(visit.id)
        })
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadVisits()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeToken() {
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
        Log.d(TAG, "Token before API call: $token")
        if (!token.isNullOrEmpty()) {
            RetrofitClient.setAuthToken(token)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun handleUnauthorized() {
        val sharedPreferencesManager = SharedPreferencesManager.getInstance(requireContext())
        sharedPreferencesManager.clearAuthData()
        // Navigate to login screen or show login dialog
        Toast.makeText(requireContext(), getString(R.string.error_unauthorized), Toast.LENGTH_LONG).show()
        // You might want to navigate to login screen here
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadVisits() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true
                val apiService = RetrofitClient.getInstance(requireContext())
                val response = apiService.getMedicalVisits()
                Log.d(TAG, "Visits loaded: ${response.size}")
                adapter.submitList(response)
                updateEmptyState(response.isEmpty())
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP exception loading visits: code=${e.code()}, message=${e.message()}", e)
                if (e.code() == 401) {
                    handleUnauthorized()
                } else {
                    Toast.makeText(requireContext(), "Error loading visits: ${e.code()}", Toast.LENGTH_SHORT).show()
                }
                updateEmptyState(true)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading visits", e)
                Toast.makeText(requireContext(), "Unexpected error", Toast.LENGTH_SHORT).show()
                updateEmptyState(true)
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteVisit(visitId: Long) {
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getInstance(requireContext())
                apiService.deleteMedicalVisit(visitId)
                Toast.makeText(requireContext(), "Visit deleted", Toast.LENGTH_SHORT).show()
                loadVisits()
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleUnauthorized()
                } else {
                    Log.e(TAG, "Failed to delete visit", e)
                    Toast.makeText(requireContext(), "Failed to delete visit", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete visit", e)
                Toast.makeText(requireContext(), "Failed to delete visit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
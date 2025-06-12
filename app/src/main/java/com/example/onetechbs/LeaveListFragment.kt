package com.example.onetechbs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentLeaveListBinding
import com.example.onetechbs.db.LeaveResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.Gson

class LeaveListFragment : Fragment() {

    private var _binding: FragmentLeaveListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var leaveListAdapter: LeaveListAdapter
    private val leaveList = mutableListOf<LeaveResponse>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaveListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        fetchLeaves()
    }

    private fun setupRecyclerView() {
        leaveListAdapter = LeaveListAdapter()
        binding.leaveRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaveListAdapter
        }
    }
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchLeaves()
        }
    }

    private fun fetchLeaves() {
        val token = RetrofitClient.getAuthToken()
        val employeeId = getEmployeeId()

        if (token.isNullOrEmpty()) {
            showError("Unauthorized: Please login again.")
            return
        }

        if (employeeId == null) {
            showError("Error: Employee ID not found")
            return
        }

        showLoading(true)
        RetrofitClient.leaveService.getEmployeeLeaves(employeeId).enqueue(object : Callback<List<LeaveResponse>> {
            override fun onResponse(call: Call<List<LeaveResponse>>, response: Response<List<LeaveResponse>>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val leaves = response.body() ?: emptyList()
                    leaveList.clear()
                    leaveList.addAll(leaves)
                    leaveListAdapter.notifyDataSetChanged()
                    updateUI()
                } else {
                    handleError(response)
                }
            }

            override fun onFailure(call: Call<List<LeaveResponse>>, t: Throwable) {
                showLoading(false)
                showError("Network error: ${t.message}")
                Log.e("LeaveListFragment", "Error: ${t.message}", t)
            }
        })
    }

    private fun getEmployeeId(): String? {
        return SharedPreferencesManager.getCurrentUserId(requireContext())

    }

    private fun handleError(response: Response<List<LeaveResponse>>) {
        val errorBody = response.errorBody()?.string()
        val errorMessage = try {
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            errorResponse.message ?: "Unknown error occurred"
        } catch (e: Exception) {
            "Error: ${response.code()}"
        }
        showError(errorMessage)
    }

    private fun updateUI() {
        if (leaveList.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.leaveRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.leaveRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = show
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ErrorResponse(
        val message: String? = null
    )
}
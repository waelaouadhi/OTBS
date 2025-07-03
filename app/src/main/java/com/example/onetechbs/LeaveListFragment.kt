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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentLeaveListBinding
import com.example.onetechbs.db.LeaveResponse
import com.example.onetechbs.db.LeaveStatus
import com.example.onetechbs.db.EStatus
import com.example.onetechbs.network.RetrofitClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import retrofit2.Response

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

    @RequiresApi(Build.VERSION_CODES.O)
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchLeaves()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLeaves() {
        val token = RetrofitClient.getAuthToken()
        if (token.isNullOrEmpty()) {
            showError("Unauthorized: Please login again.")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.leaveService.getLeaveHistory("Bearer $token")
                }
                showLoading(false)
                if (response.isSuccessful) {
                    val leaves = response.body() ?: emptyList()
                    // Convert API Leave model to UI LeaveResponse model expected by adapter
                    val converted = leaves.map { leave ->
                        LeaveResponse(
                            id = leave.id ?: 0L,
                            name = leave.userDn.substringBefore(","),
                            department = "",
                            startDate = leave.startDate,
                            endDate = leave.endDate,
                            leaveType = leave.leaveType,
                            status = when (leave.status) {
                                EStatus.APPROUVÉE -> LeaveStatus.APPROVED
                                EStatus.REFUSÉE -> LeaveStatus.REJECTED
                                else -> LeaveStatus.PENDING
                            }
                        )
                    }
                    leaveList.clear()
                    leaveList.addAll(converted)
                    leaveListAdapter.submitList(converted.toList())
                    updateUI()
                } else {
                    showError("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                showLoading(false)
                showError("Network error: ${e.message}")
                Log.e("LeaveListFragment", "Error", e)
            }
        }
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
}
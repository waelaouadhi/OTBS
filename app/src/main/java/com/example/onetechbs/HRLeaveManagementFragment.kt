package com.example.onetechbs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentHrLeaveManagementBinding
import com.example.onetechbs.db.EStatus
import com.example.onetechbs.db.Leave
import com.example.onetechbs.db.MessageResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate

class HRLeaveManagementFragment : Fragment() {

    private var _binding: FragmentHrLeaveManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: LeaveRequestsAdapter
    private var allLeaves: List<Leave> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHrLeaveManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Fragment view created")
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilterRadioGroup()
        fetchLeaveRequests()
    }

    private fun setupFilterRadioGroup() {
        binding.filterRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioAll -> filterLeaves(null)
                R.id.radioPending -> filterLeaves(EStatus.EN_ATTENTE)
                R.id.radioRejected -> filterLeaves(EStatus.REFUSÉE)
                R.id.radioAccepted -> filterLeaves(EStatus.APPROUVÉE)
            }
        }
    }

    private fun filterLeaves(status: EStatus?) {
        val filteredList = if (status == null) {
            allLeaves
        } else {
            allLeaves.filter { it.status == status }
        }
        adapter.submitList(filteredList)
        updateUI(filteredList.isEmpty())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        adapter = LeaveRequestsAdapter(
            onItemClick = { leave ->
                Log.d(TAG, "Leave item clicked: ID=${leave.id}")
                showLeaveDetails(leave)
            },
            onApprove = { leave ->
                Log.d(TAG, "Approving leave ID=${leave.id}")
                handleApprove(leave)
            },
            onReject = { leave ->
                Log.d(TAG, "Rejecting leave ID=${leave.id}")
                if (leave.id != null) {
                    handleRejectWithValidation(leave)
                }
            },
            onRequestPermission = {
                requestStoragePermission()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HRLeaveManagementFragment.adapter
            setHasFixedSize(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Swipe-to-refresh triggered")
            fetchLeaveRequests()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLeaveRequests() {
        val context = requireContext()
        val authToken = SharedPreferencesManager.getInstance(context).getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Log.e(TAG, "Auth token is missing")
            showError("Session expired. Please log in again.")
            return
        }

        Log.d(TAG, "Auth token retrieved: $authToken")
        RetrofitClient.setAuthToken(authToken)
        binding.swipeRefresh.isRefreshing = true
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching leave requests from server...")
                val response = RetrofitClient.leaveService.getLeaves(
                    page = 0,
                    size = 100, // Fetch more items for better UX
                    sort = "createdAt,desc",
                    token = "Bearer $authToken",
                )
                if (response.isSuccessful) {
                    val leavePageResponse = response.body()
                    allLeaves = leavePageResponse?.content?.sortedByDescending { it.createdAt } ?: emptyList()

                    // Apply current filter
                    val checkedId = binding.filterRadioGroup.checkedRadioButtonId
                    when (checkedId) {
                        R.id.radioAll -> filterLeaves(null)
                        R.id.radioPending -> filterLeaves(EStatus.EN_ATTENTE)
                        R.id.radioRejected -> filterLeaves(EStatus.REFUSÉE)
                        R.id.radioAccepted -> filterLeaves(EStatus.APPROUVÉE)
                    }
                } else {
                    showError("Failed to fetch leave requests: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching leaves: ${e.message}", e)
                showError("Something went wrong. Please check your internet connection.")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateUI(isEmpty: Boolean) {
        if (isEmpty) {
            Log.d(TAG, "No leave requests to display - showing empty state")
            binding.recyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            Log.d(TAG, "Leave requests loaded - showing list")
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    private fun showLeaveDetails(leave: Leave) {
        Toast.makeText(context, "Viewing details for Leave ID: ${leave.id}", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleApprove(leave: Leave) {
        val today = LocalDate.now()

        // Validate dates
        if (leave.startDate != null && leave.startDate.isBefore(today)) {
            Toast.makeText(requireContext(), "Start date is in the past ❌", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.endDate != null && leave.endDate.isBefore(today.plusDays(1))) {
            Toast.makeText(requireContext(), "End date must be in the future ❌", Toast.LENGTH_SHORT).show()
            return
        }

        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Token missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.leaveService.approveLeave(leave.id!!, "Bearer $token")
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Leave approved ✅", Toast.LENGTH_SHORT).show()
                        fetchLeaveRequests() // Refresh list after approval
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRejectWithValidation(leave: Leave) {
        val today = LocalDate.now()

        // Check if leave end date is in the past
        if (leave.endDate != null && leave.endDate.isBefore(today)) {
            // Show alert or Toast to user that reject is not possible
            Toast.makeText(requireContext(), "Cannot reject leave: leave period is in the past.", Toast.LENGTH_LONG).show()
            return
        }

        // If validation passes, proceed with actual reject
        leave.id?.let { handleReject(it) }
    }

    private fun handleReject(leaveId: Long) {
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Token missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.leaveService.rejectLeave(leaveId, "Bearer $token")
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Leave rejected ❌", Toast.LENGTH_SHORT).show()
                        fetchLeaveRequests() // Refresh list after rejection
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Fragment view destroyed")
        _binding = null
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Android 10 and above use scoped storage
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Storage permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val TAG = "HRLeaveManagementFragment"
        private const val STORAGE_PERMISSION_CODE = 100
    }
}
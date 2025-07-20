package com.example.onetechbs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.example.onetechbs.db.LeaveStatus
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

        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            try {
                androidx.navigation.Navigation.findNavController(view)
                    .popBackStack(R.id.homefraFragment, false)
            } catch (e: Exception) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

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
        val filteredList = if (status == null) allLeaves else allLeaves.filter { it.status == status }
        adapter.submitList(filteredList)
        updateUI(filteredList.isEmpty())
    }

    private fun applyCurrentFilter() {
        when (binding.filterRadioGroup.checkedRadioButtonId) {
            R.id.radioAll -> filterLeaves(null)
            R.id.radioPending -> filterLeaves(EStatus.EN_ATTENTE)
            R.id.radioRejected -> filterLeaves(EStatus.REFUSÉE)
            R.id.radioAccepted -> filterLeaves(EStatus.APPROUVÉE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        adapter = LeaveRequestsAdapter(
            onItemClick = { leave -> showLeaveDetails(leave) },
            onApprove = { leave -> handleApprove(leave) },
            onReject = { leave -> leave.id?.let { handleRejectWithValidation(leave) } },
            onRequestPermission = { requestStoragePermission() }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HRLeaveManagementFragment.adapter
            setHasFixedSize(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { fetchLeaveRequests() }
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
                val response = RetrofitClient.leaveService.getAllReceivedLeaves("Bearer $authToken")
                if (response.isSuccessful) {
                    val leaveResponseList = response.body() ?: emptyList()

                    allLeaves = leaveResponseList
    .filter { it.id != null && it.startDate != null && it.endDate != null && it.name != null }
    .map { responseItem ->
        Leave(
            id = responseItem.id,
            userDn = responseItem.name ?: "Unknown",
            startDate = responseItem.startDate,
            endDate = responseItem.endDate,
            leaveType = responseItem.leaveType,
            status = when (responseItem.status) {
    EStatus.EN_ATTENTE -> EStatus.EN_ATTENTE
    EStatus.APPROUVÉE -> EStatus.APPROUVÉE
    EStatus.REFUSÉE -> EStatus.REFUSÉE
    EStatus.PENDING -> EStatus.EN_ATTENTE // fallback mapping
    EStatus.CONFIRMED -> EStatus.APPROUVÉE // fallback mapping
    else -> EStatus.EN_ATTENTE // default fallback
},
            startTime = null,
            endTime = null,
            attachment = null,
            createdAt = null,
            updatedAt = null
        )
    }
    .sortedByDescending { it.startDate }

                    applyCurrentFilter()
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
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showLeaveDetails(leave: Leave) {
        Toast.makeText(context, "Viewing details for Leave ID: ${leave.id}", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleApprove(leave: Leave) {
        val today = LocalDate.now()
        if (leave.startDate == null) {
            Toast.makeText(requireContext(), "Missing start date.", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.endDate == null) {
            Toast.makeText(requireContext(), "Missing end date.", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.startDate.isBefore(today)) {
            Toast.makeText(requireContext(), "Start date is in the past ❌", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.endDate.isBefore(today.plusDays(1))) {
            Toast.makeText(requireContext(), "End date must be in the future ❌", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.id == null) {
            Toast.makeText(requireContext(), "Invalid leave ID.", Toast.LENGTH_SHORT).show()
            return
        }
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Token missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.leaveService.approveLeave(leave.id, "Bearer $token")
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Leave approved ✅", Toast.LENGTH_SHORT).show()
                        fetchLeaveRequests()
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
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
        if (leave.endDate == null) {
            Toast.makeText(requireContext(), "Missing end date.", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.id == null) {
            Toast.makeText(requireContext(), "Invalid leave ID.", Toast.LENGTH_SHORT).show()
            return
        }
        if (leave.endDate.isBefore(today)) {
            Toast.makeText(requireContext(), "Cannot reject leave: leave period is in the past.", Toast.LENGTH_LONG).show()
            return
        }
        handleReject(leave.id)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
                        fetchLeaveRequests()
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
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
        _binding = null
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
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
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Storage permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "HRLeaveManagementFragment"
        private const val STORAGE_PERMISSION_CODE = 100
    }
}
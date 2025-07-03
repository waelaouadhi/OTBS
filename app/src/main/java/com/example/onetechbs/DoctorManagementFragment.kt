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
import com.example.onetechbs.HRLeaveManagementFragment.Companion
import com.example.onetechbs.adapter.DoctorManagementAdapter
import com.example.onetechbs.databinding.FragmentDoctorManagementBinding
import com.example.onetechbs.db.MedicalVisitResponse
import com.example.onetechbs.DoctorFragment
import com.example.onetechbs.db.MessageResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response

class DoctorManagementFragment : Fragment() {

    private var _binding: FragmentDoctorManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DoctorManagementAdapter
private var allVisits: List<MedicalVisitResponse> = emptyList()
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

        // --- Search Bar Logic ---
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                filterVisits(query)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // --- FAB Navigation ---
        binding.fabAddDoctor.setOnClickListener {
            navigateToAddDoctor()
        }
        // Also handle the empty state addDoctorButton
        binding.addDoctorButton?.setOnClickListener {
            navigateToAddDoctor()
        }
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
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyTitleTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.emptySubtitleTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        // Only set this if the addDoctorButton exists in your layout
        try {
            binding.addDoctorButton?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        } catch (_: Exception) {}
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
        val context = requireContext()
        val authToken = SharedPreferencesManager.getInstance(context).getAuthToken()
        if (authToken.isNullOrEmpty()) {
            Log.e(TAG, "Auth token is null or empty")
            Toast.makeText(context, "Authentication token is missing. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(Companion.TAG, "Auth token retrieved: $authToken")
        RetrofitClient.setAuthToken(authToken)

        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true

                val response = RetrofitClient.medService.getMedicalVisits("Bearer $authToken")

                allVisits = response
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
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Token missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.medService.deleteMedicalVisit(visitId, "Bearer $token")
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Visit deleted ✅", Toast.LENGTH_SHORT).show()
                        loadVisits()
                    } else if (response.code() == 401) {
                        handleUnauthorized()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error deleting visit: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Network error when deleting visit", t)
                }
            })
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Filtering Logic for Search Bar ---
    private fun filterVisits(query: String) {
        val filtered = if (query.isEmpty()) {
            allVisits
        } else {
            allVisits.filter { it.doctorName.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    // --- Navigation to Add Doctor Fragment ---
    private fun navigateToAddDoctor() {
        // Use FragmentManager to navigate to DoctorFragment
        val fragment = DoctorFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_layout, fragment)
            .addToBackStack(null)
            .commit()
    }
}
package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentDoctorListBinding
import com.example.onetechbs.db.AppointmentRequest
import com.example.onetechbs.db.MedicalVisitResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DoctorListFragment : Fragment() {

    private var _binding: FragmentDoctorListBinding? = null
    private val binding get() = _binding!!

    private lateinit var doctorVisitAdapter: DoctorVisitAdapter
    private var searchJob: Job? = null
    private var currentQuery = ""
    private val selectedFilters = mutableSetOf<String>()
    private var allDoctors = listOf<MedicalVisitResponse>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // back navigation
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        


        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        setupFilterChips()
        setupRetryButton()
        fetchDoctorVisits()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        doctorVisitAdapter = DoctorVisitAdapter { doctor, selectedTime -> submitMedicalVisit(doctor, selectedTime) }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = doctorVisitAdapter
            setHasFixedSize(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitMedicalVisit(doctor: MedicalVisitResponse, selectedTime: String) {
        val context = requireContext()
        val token = SharedPreferencesManager.getInstance(context).getAuthToken()

        if (token.isNullOrBlank()) {
            Toast.makeText(context, "Authentication token missing. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert selectedTime to proper ISO format (e.g., "2025-06-20T10:00:00")
        val formattedDateTime = "${doctor.visitDate}T$selectedTime"

        val appointmentRequest = AppointmentRequest(
            medicalVisitId = doctor.id, // assuming doctor has `id: Long`
            timeSlot = formattedDateTime
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.medService.submitAppointment("Bearer $token", appointmentRequest)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Appointment scheduled successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Failed to schedule: ${response.code()} ${response.message()} ${errorBody ?: ""}", Toast.LENGTH_LONG).show()
                    Log.e("DoctorListFragment", "API error: $errorBody")
                }
            } catch (e: HttpException) {
                Toast.makeText(context, "Server error: ${e.message()}", Toast.LENGTH_LONG).show()
                Log.e("DoctorListFragment", "HttpException: ${e.message()}")
            } catch (e: IOException) {
                Toast.makeText(context, "Network error: Check your connection.", Toast.LENGTH_LONG).show()
                Log.e("DoctorListFragment", "IOException: ${e.message}")
            } catch (e: Exception) {
                Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("DoctorListFragment", "Exception: ${e.message}")
            }
        }
    }
    private fun calculateEndTime(startTime: String): String {
        return try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.parse(startTime)
            val calendar = Calendar.getInstance()
            if (time != null) {
                calendar.time = time
                calendar.add(Calendar.HOUR, 1)
            }
            timeFormat.format(calendar.time)
        } catch (e: Exception) {
            startTime // fallback to startTime if parsing fails
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // debounce
                    currentQuery = newText ?: ""
                    filterDoctors()
                }
                return true
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchDoctorVisits()
        }
    }

    private fun setupFilterChips() {
        val doctors = listOf("dwael", "Dr. Smith", "Dr. Brown", "Dr. Davis", "Dr. Wilson")

        doctors.forEach { doctorName ->
            val chip = Chip(requireContext()).apply {
                text = doctorName
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedFilters.add(doctorName) else selectedFilters.remove(doctorName)
                    filterDoctors()
                }
            }
            binding.filterChipGroup.addView(chip)
        }
    }

    private fun filterDoctors() {
        val filteredList = allDoctors.filter { doctor ->
            val matchesSearch = currentQuery.isEmpty() || doctor.doctorName.contains(currentQuery, ignoreCase = true)
            val matchesFilter = selectedFilters.isEmpty() || selectedFilters.contains(doctor.doctorName)
            matchesSearch && matchesFilter
        }
        doctorVisitAdapter.submitList(filteredList)
        showEmptyState(filteredList.isEmpty())
    }

    private fun showEmptyState(show: Boolean) {
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDoctorVisits() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.medService.getDoctorVisits()
                binding.swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    response.body()?.let {
                        allDoctors = it
                        filterDoctors()
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
                handleError("Server error: ${e.message()}", e)
            } catch (e: IOException) {
                handleError("Check your internet connection.", e)
            } catch (e: Exception) {
                handleError("An unexpected error occurred: ${e.message}", e)
            }
        }
    }

    private fun handleError(message: String, error: Exception) {
        Log.e("DoctorListFragment", "Error: ${error.message}")
        binding.swipeRefreshLayout.isRefreshing = false
        showLoading(false)
        showError(true, message)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(show: Boolean, message: String = "") {
        binding.errorContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.errorTextView.text = message
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            fetchDoctorVisits()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
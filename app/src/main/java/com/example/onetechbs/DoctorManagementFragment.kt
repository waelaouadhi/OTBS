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
import com.example.onetechbs.adapter.AppointmentDialogAdapter
import com.example.onetechbs.db.MedicalVisitRequest
import com.example.onetechbs.db.MessageResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.appbar.MaterialToolbar
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
    private var searchJob: kotlinx.coroutines.Job? = null
    private val searchDebounceTime = 300L // milliseconds

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

        // Back navigation
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            try {
                androidx.navigation.Navigation.findNavController(view)
                    .popBackStack(R.id.homefraFragment, false)
            } catch (e: Exception) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        loadVisits()

        // --- Search Bar Setup ---
        setupSearchView()

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
        adapter = DoctorManagementAdapter(
            onDeleteClicked = { visit -> 
                // Show confirmation dialog before deleting
                showDeleteConfirmationDialog(visit)
            },
            onUpdateClicked = { visit ->
                // Show update dialog
                showUpdateDialog(visit)
            },
            onCardClicked = { visit -> showDoctorAppointmentsDialog(visit) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        // Attach swipe actions (delete left, update right)
        adapter.attachSwipeActions(binding.recyclerView)
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDeleteConfirmationDialog(visit: MedicalVisitResponse) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Doctor Visit")
            .setMessage("Are you sure you want to delete this doctor visit?")
            .setPositiveButton("Delete") { _, _ ->
                deleteVisit(visit.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDoctorAppointmentsDialog(visit: MedicalVisitResponse) {
        lifecycleScope.launch {
            val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
            if (token.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Token missing. Please login again.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val response = RetrofitClient.medService.getAppointmentsByMedVisitId(visit.id.toString())
                if (response.isSuccessful) {
                    val appointments = response.body() ?: emptyList()
                    appointments.forEachIndexed { idx, appt ->
                        Log.e("AppointmentDebug", "[$idx] Appointment: $appt")
                        Log.e("AppointmentDebug", "[$idx] employeeFullName: ${appt.employeeFullName}, employeeEmail: ${appt.employeeEmail}, timeSlot: ${appt.timeSlot}")
                    }
                    if (appointments.isEmpty()) {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Appointments for ${visit.doctorName}")
                            .setMessage("No appointments for this doctor.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        val dialogView = layoutInflater.inflate(R.layout.dialog_appointments, null)
                        val rv = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAppointments)
                        val btnClose = dialogView.findViewById<android.widget.Button>(R.id.btnClose)
                        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
                        tvTitle.text = "Appointments for ${visit.doctorName}"
                        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                        rv.adapter = AppointmentDialogAdapter(appointments)
                        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setView(dialogView)
                            .create()
                        btnClose.setOnClickListener { dialog.dismiss() }
                        dialog.show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch appointments", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
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
        
        // Show/hide swipe hint animation
        try {
            binding.swipeHintAnimation.visibility = if (isEmpty) View.VISIBLE else View.GONE
            if (isEmpty) {
                binding.swipeHintAnimation.playAnimation()
            } else {
                binding.swipeHintAnimation.cancelAnimation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with swipe hint animation", e)
        }
        
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
    if (!isAdded) return
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showUpdateDialog(visit: MedicalVisitResponse) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_medical_visit, null)
        
        val doctorNameEditText = dialogView.findViewById<android.widget.EditText>(R.id.editTextDoctorName)
        val visitDateEditText = dialogView.findViewById<android.widget.EditText>(R.id.editTextVisitDate)
        val startTimeEditText = dialogView.findViewById<android.widget.EditText>(R.id.editTextStartTime)
        val endTimeEditText = dialogView.findViewById<android.widget.EditText>(R.id.editTextEndTime)
        
        // Pre-fill with current values
        doctorNameEditText.setText(visit.doctorName)
        visitDateEditText.setText(visit.visitDate)
        startTimeEditText.setText(visit.startTime)
        endTimeEditText.setText(visit.endTime)
        
        // Set up date picker for visit date
        visitDateEditText.setOnClickListener {
            val datePicker = android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    visitDateEditText.setText(selectedDate)
                },
                java.time.LocalDate.parse(visit.visitDate).year,
                java.time.LocalDate.parse(visit.visitDate).monthValue - 1,
                java.time.LocalDate.parse(visit.visitDate).dayOfMonth
            )
            datePicker.show()
        }
        
        // Set up time pickers
        startTimeEditText.setOnClickListener {
            val timePicker = android.app.TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val selectedTime = String.format("%02d:%02d:00", hourOfDay, minute)
                    startTimeEditText.setText(selectedTime)
                },
                java.time.LocalTime.parse(visit.startTime).hour,
                java.time.LocalTime.parse(visit.startTime).minute,
                true
            )
            timePicker.show()
        }
        
        endTimeEditText.setOnClickListener {
            val timePicker = android.app.TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val selectedTime = String.format("%02d:%02d:00", hourOfDay, minute)
                    endTimeEditText.setText(selectedTime)
                },
                java.time.LocalTime.parse(visit.endTime).hour,
                java.time.LocalTime.parse(visit.endTime).minute,
                true
            )
            timePicker.show()
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Update Medical Visit")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updatedVisit = MedicalVisitRequest(
                    doctorName = doctorNameEditText.text.toString().trim(),
                    visitDate = visitDateEditText.text.toString().trim(),
                    startTime = startTimeEditText.text.toString().trim(),
                    endTime = endTimeEditText.text.toString().trim()
                )
                
                if (validateUpdateInput(updatedVisit)) {
                    updateVisit(visit.id, updatedVisit)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun validateUpdateInput(visit: MedicalVisitRequest): Boolean {
        if (visit.doctorName.isEmpty()) {
            Toast.makeText(requireContext(), "Doctor name is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (visit.visitDate.isEmpty()) {
            Toast.makeText(requireContext(), "Visit date is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (visit.startTime.isEmpty()) {
            Toast.makeText(requireContext(), "Start time is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (visit.endTime.isEmpty()) {
            Toast.makeText(requireContext(), "End time is required", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateVisit(visitId: Long, updatedVisit: MedicalVisitRequest) {
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Token missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.medService.updateMedicalVisit(visitId, "Bearer $token", updatedVisit)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Visit updated successfully ✅", Toast.LENGTH_SHORT).show()
                        loadVisits()
                    } else if (response.code() == 401) {
                        handleUnauthorized()
                    } else {
                        val errorMessage = try {
                            response.errorBody()?.string() ?: "Unknown error"
                        } catch (e: Exception) {
                            "Error: ${response.code()}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Error updating visit: $errorMessage")
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Network error when updating visit", t)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Search View Setup ---
    private fun setupSearchView() {
        // Clear any existing text change listeners
        binding.searchEditText.clearFocus()
        
        // Set up search icon click listener
        binding.searchEditText.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.searchEditText.right - binding.searchEditText.compoundDrawables[2].bounds.width())) {
                    // Clicked on the clear icon
                    binding.searchEditText.text?.clear()
                    filterVisits("")
                    return@setOnTouchListener true
                }
            }
            false
        }
        
        // Set up search input listener with debounce
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                searchJob = kotlinx.coroutines.MainScope().launch {
                    s?.toString()?.let { query ->
                        kotlinx.coroutines.delay(searchDebounceTime)
                        if (query != binding.searchEditText.text.toString()) {
                            return@launch
                        }
                        filterVisits(query.trim())
                    } ?: filterVisits("")
                }
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // Handle search action from keyboard
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text?.toString()?.trim() ?: ""
                filterVisits(query)
                // Hide keyboard after search
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
                binding.searchEditText.clearFocus()
                true
            } else {
                false
            }
        }
    }
    
    // --- Filtering Logic for Search Bar ---
    private fun filterVisits(query: String) {
        val filtered = if (query.isEmpty()) {
            allVisits
        } else {
            allVisits.filter { 
                it.doctorName.contains(query, ignoreCase = true)
            }
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
package com.example.onetechbs.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentAttendanceBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceFragment : Fragment() {
    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupDatePicker()
        fetchAttendanceRecords(null)
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.datePickerButton.setOnClickListener {
            // Show a date picker dialog and fetch records for selected date
            val today = LocalDate.now()
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(today.toEpochDay() * 24 * 60 * 60 * 1000)
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = LocalDate.ofEpochDay(selection / (24 * 60 * 60 * 1000))
                fetchAttendanceRecords(selectedDate.format(DateTimeFormatter.ISO_DATE))
            }
            datePicker.show(parentFragmentManager, "datePicker")
        }
    }

    private fun fetchAttendanceRecords(date: String?) {
        lifecycleScope.launch {
            try {
                val api = AttendanceRetrofitClient.apiService
                val records = api.getAllAttendanceRecords(date)
                adapter.submitList(records)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load attendance: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.onetechbs

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.onetechbs.db.MedicalVisitRequest
import com.example.onetechbs.network.RetrofitClient
import androidx.lifecycle.lifecycleScope
import com.example.onetechbs.util.SharedPreferencesManager
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

class DoctorFragment : Fragment() {

    private lateinit var editDoctorName: TextInputEditText
    private lateinit var compactCalendarView: CompactCalendarView
    private lateinit var editStartTime: TextInputEditText
    private lateinit var editEndTime: TextInputEditText
    private lateinit var submitbtn: MaterialButton
    private lateinit var selectedDateTextView: MaterialTextView
    private lateinit var doctorVisitText: MaterialTextView
    private lateinit var progressBar: View

    private var selectedDate: Calendar? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor, container, false)

        // Initialize views
        editDoctorName = view.findViewById(R.id.editDoctorName)
        compactCalendarView = view.findViewById(R.id.compactCalendarView)
        editStartTime = view.findViewById(R.id.editStartTime)
        editEndTime = view.findViewById(R.id.editEndTime)
        submitbtn = view.findViewById(R.id.submitbtn)
        selectedDateTextView = view.findViewById(R.id.selectedDateValue)
        doctorVisitText = view.findViewById(R.id.doctorVisitText)
        progressBar = view.findViewById(R.id.progressBar)

        // Setup time pickers
        setupTimePicker(editStartTime, "Start Time")
        setupTimePicker(editEndTime, "End Time")

        compactCalendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(date: Date?) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                selectedDate = calendar

                val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(calendar.time)
                selectedDateTextView.text = formattedDate
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {}
        })

        submitbtn.setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val doctorName = editDoctorName.text.toString().trim()
            val startTime = editStartTime.text.toString().trim()
            val endTime = editEndTime.text.toString().trim()

            if (doctorName.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate time range
            if (!isValidTimeRange(startTime, endTime)) {
                Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!.time)
            val token = context?.let { it1 -> SharedPreferencesManager.getInstance(it1).getAuthToken() }

            val request = MedicalVisitRequest(
                doctorName = doctorName,
                visitDate = formattedDate,
                startTime = startTime,
                endTime = endTime
            )

            lifecycleScope.launch {
                try {
                    progressBar.visibility = View.VISIBLE
                    submitbtn.isEnabled = false

                    val response = RetrofitClient.medService.submitMedicalVisit("Bearer $token", request)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Appointment scheduled!", Toast.LENGTH_SHORT).show()

                        val event = Event(Color.RED, selectedDate!!.timeInMillis, "Appointment with $doctorName")
                        compactCalendarView.addEvent(event)

                        // Clear form
                        editDoctorName.text?.clear()
                        editStartTime.text?.clear()
                        editEndTime.text?.clear()
                        selectedDateTextView.text = "Not selected"
                        selectedDate = null

                        sendAppointmentNotification(doctorName, formattedDate, startTime)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            requireContext(),
                            "Server error: ${errorBody ?: response.message()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    progressBar.visibility = View.GONE
                    submitbtn.isEnabled = true
                }
            }
        }

        return view
    }

    private fun setupTimePicker(editText: TextInputEditText, title: String) {
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val time = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    editText.setText(timeFormat.format(time.time))
                },
                currentHour,
                currentMinute,
                true // 24-hour format
            ).show()
        }
    }

    private fun isValidTimeRange(startTime: String, endTime: String): Boolean {
        try {
            val start = timeFormat.parse(startTime)
            val end = timeFormat.parse(endTime)
            return start != null && end != null && end.after(start)
        } catch (e: Exception) {
            return false
        }
    }

    private fun sendAppointmentNotification(doctorName: String, visitDate: String, startTime: String) {
        val notificationTitle = "New Appointment Scheduled"
        val notificationMessage = "Doctor: $doctorName, Date: $visitDate, Time: $startTime"
        Toast.makeText(requireContext(), "Notification sent: $notificationMessage", Toast.LENGTH_SHORT).show()
    }
}
package com.example.onetechbs

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.onetechbs.db.MedicalVisitRequest
import com.example.onetechbs.network.RetrofitClient
import androidx.lifecycle.lifecycleScope
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DoctorFragment : Fragment() {

    private lateinit var compactCalendarView: CompactCalendarView
    private lateinit var selectedDateText: TextView
    private lateinit var submitBtn: ImageButton
    private lateinit var tagView: View
    private lateinit var doctorNameEdit: EditText
    private lateinit var startTimeEdit: EditText
    private lateinit var endTimeEdit: EditText

    private var selectedDate: Calendar? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor, container, false)

        // Initialize views
        compactCalendarView = view.findViewById(R.id.compactCalendarView)
        selectedDateText = view.findViewById(R.id.textView10)
        submitBtn = view.findViewById(R.id.submitbtn)
        tagView = view.findViewById(R.id.dateTag)
        doctorNameEdit = view.findViewById(R.id.editDoctorName)
        startTimeEdit = view.findViewById(R.id.editStartTime)
        endTimeEdit = view.findViewById(R.id.editEndTime)

        tagView.visibility = View.GONE

        compactCalendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(date: Date?) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                selectedDate = calendar

                val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(calendar.time)
                selectedDateText.text = "You chose this date: $formattedDate\nFor visiting the doctor, please be on time."

                tagView.visibility = View.GONE
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {}
        })

        submitBtn.setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val doctorName = doctorNameEdit.text.toString().trim()
            val startTime = startTimeEdit.text.toString().trim()
            val endTime = endTimeEdit.text.toString().trim()

            if (doctorName.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!.time)

            val request = MedicalVisitRequest(
                doctorName = doctorName,
                visitDate = formattedDate,
                startTime = startTime,
                endTime = endTime
            )

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.medService.submitMedicalVisit(request)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Appointment scheduled!", Toast.LENGTH_SHORT).show()

                        val event = Event(Color.RED, selectedDate!!.timeInMillis, "Appointment with $doctorName")
                        compactCalendarView.addEvent(event)

                        tagView.setBackgroundColor(Color.parseColor("#FFA500"))
                        tagView.layoutParams.height = 20
                        tagView.layoutParams.width = 20
                        tagView.visibility = View.VISIBLE

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
                }
            }
        }

        return view
    }

    private fun sendAppointmentNotification(doctorName: String, visitDate: String, startTime: String) {
        val notificationTitle = "New Appointment Scheduled"
        val notificationMessage = "Doctor: $doctorName, Date: $visitDate, Time: $startTime"
        Toast.makeText(requireContext(), "Notification sent: $notificationMessage", Toast.LENGTH_SHORT).show()
    }
}
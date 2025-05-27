package com.example.onetechbs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import java.text.SimpleDateFormat
import java.util.*

class DoctorSchedulingFragment : Fragment() {

    private lateinit var compactCalendarView: CompactCalendarView
    private lateinit var cancelButton: Button
    private lateinit var submitButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout
        val view = inflater.inflate(R.layout.doctorschedulingfragment, container, false)

        // Initialize views
        compactCalendarView = view.findViewById(R.id.calendarView)
        cancelButton = view.findViewById(R.id.cancelButton)
        submitButton = view.findViewById(R.id.submitButton)

        // Cancel Button: Reset the calendar or close the fragment (for testing purposes)
        cancelButton.setOnClickListener {
            // Reset the calendar view (you can add any behavior you need here)
            compactCalendarView.removeAllEvents() // Clears any selected date (if any)
        }

        // Submit Button: Get the selected date and navigate to ManagerApprovalActivity
        submitButton.setOnClickListener {
            val selectedDate = compactCalendarView.firstDayOfCurrentMonth
            // Get the selected date from the calendar
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = format.format(selectedDate)

            // Save the selected date using SharedPreferences
            val sharedPreferences = requireActivity().getSharedPreferences("DoctorSchedule", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("selectedDate", formattedDate)
            editor.apply()

            // Show the selected date
            Toast.makeText(requireContext(), "Selected Date: $formattedDate", Toast.LENGTH_SHORT).show()

            // Navigate to ManagerApprovalActivity and pass the selected date
            val intent = Intent(requireContext(), ManagerApprovalActivity::class.java)
            intent.putExtra("selectedDate", formattedDate) // Pass the selected date to ManagerApprovalActivity
            startActivity(intent)
        }

        return view
    }
}
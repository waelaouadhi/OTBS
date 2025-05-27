package com.example.onetechbs

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ManagerApprovalActivity : AppCompatActivity() {

    private lateinit var approveButton: Button
    private lateinit var rejectButton: Button
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_approval)

        approveButton = findViewById(R.id.approveButton)
        rejectButton = findViewById(R.id.rejectButton)

        // Retrieve the selected date from the intent
        selectedDate = intent.getStringExtra("selectedDate")

        // Display the selected date (optional)
        Toast.makeText(this, "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()

        // Approve button logic
        approveButton.setOnClickListener {
            // Save approval state in SharedPreferences
            val sharedPreferences = getSharedPreferences("doctorPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("doctorApproved", true) // Store approval state
            editor.apply()

            // Display approval message
            Toast.makeText(this, "Date Approved: $selectedDate", Toast.LENGTH_SHORT).show()

            // Navigate back to the home activity
            finish() // This will finish this activity and return to the previous one
        }

        // Reject button logic
        rejectButton.setOnClickListener {
            // Save rejection state in SharedPreferences
            val sharedPreferences = getSharedPreferences("doctorPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("doctorApproved", false) // Store rejection state
            editor.apply()

            // Display rejection message
            Toast.makeText(this, "Date Rejected: $selectedDate", Toast.LENGTH_SHORT).show()

            // Navigate back to the home activity
            finish() // This will finish this activity and return to the previous one
        }
    }
}
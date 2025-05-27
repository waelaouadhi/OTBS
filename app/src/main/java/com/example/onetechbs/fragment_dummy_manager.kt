package com.example.onetechbs

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.onetechbs.databinding.FragmentDummyManagerBinding

class FragmentDummyManager : Fragment() {
    private lateinit var binding: FragmentDummyManagerBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout using the correct binding
        binding = FragmentDummyManagerBinding.inflate(inflater, container, false)
        sharedPrefs = requireContext().getSharedPreferences("doctorPrefs", AppCompatActivity.MODE_PRIVATE)

        // Get the current doctor approval status
        val isDoctorApproved = sharedPrefs.getBoolean("doctorApproved", false)
        binding.toggleButton.text = if (isDoctorApproved) "Disapprove Doctor" else "Approve Doctor"

        // Toggle doctor approval status when the button is clicked
        binding.toggleButton.setOnClickListener {
            val newStatus = !isDoctorApproved
            sharedPrefs.edit().putBoolean("doctorApproved", newStatus).apply()

            // Update the button text based on new status
            binding.toggleButton.text = if (newStatus) "Disapprove Doctor" else "Approve Doctor"
        }

        return binding.root
    }
}
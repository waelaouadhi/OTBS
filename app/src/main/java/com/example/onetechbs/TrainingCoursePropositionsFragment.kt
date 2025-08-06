package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.FragmentTrainingCoursePropositionsBinding
import android.widget.Toast
import android.widget.RadioButton
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class TrainingCoursePropositionsFragment : Fragment() {
    private var _binding: FragmentTrainingCoursePropositionsBinding? = null
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrainingCoursePropositionsBinding.inflate(inflater, container, false)
        val view = binding.root

        // Setup Toolbar with back button
        binding.toolbar.apply {
            title = "Training Course Proposition"
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        // Handle Submit Button
        binding.btnSubmit.setOnClickListener {
            val title = binding.inputCourseTitle.text.toString().trim()
            val desc = binding.inputDescription.text.toString().trim()
            val justification = binding.inputJustification.text.toString().trim()
            val outcomes = binding.inputExpectedOutcomes.text.toString().trim()
            val provider = binding.inputPreferredProvider.text.toString().trim()
            val certRequired = when (binding.radioGroupCertification.checkedRadioButtonId) {
                binding.radioYes.id -> true
                binding.radioNo.id -> false
                else -> null
            }

            // Validate required fields
            if (title.isEmpty() || desc.isEmpty() || justification.isEmpty() || outcomes.isEmpty() || provider.isEmpty() || certRequired == null) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call API to propose course
            lifecycleScope.launch {
                try {
                    // Show loading (optional: you can add a ProgressBar)
                    val request = com.example.onetechbs.db.CoursePropositionRequestDTO(
                        title = title,
                        description = desc,
                        justification = justification,
                        expectedOutcomes = outcomes,
                        isCertified = certRequired,
                        preferredProvider = provider
                    )
                    val response = com.example.onetechbs.network.RetrofitClient.proposeCourseProposition(requireContext(), request)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Course proposition submitted!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Failed to submit: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Handle Cancel Button
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

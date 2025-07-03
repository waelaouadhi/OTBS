package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.FragmentInternalRecruitingBinding
import com.example.onetechbs.db.JobOfferRequest
import com.example.onetechbs.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InternalRecruitingFragment : Fragment() {

    private var _binding: FragmentInternalRecruitingBinding? = null
    private val binding get() = _binding!!

    private val departments = listOf("Select a department", "HR", "Engineering", "Marketing", "Sales")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInternalRecruitingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, departments)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.departmentSpinner.adapter = adapter

        // Back navigation via toolbar
        binding.toolbar.setNavigationOnClickListener {
            try {
                androidx.navigation.Navigation.findNavController(view)
                    .popBackStack(R.id.homefraFragment, false)
            } catch (e: Exception) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                val jobOfferRequest = JobOfferRequest(
                    title = binding.jobTitle.text.toString().trim(),
                    department = binding.departmentSpinner.selectedItem.toString(),
                    description = binding.description.text.toString().trim(),
                    responsibilities = binding.responsibilities.text.toString().trim(),
                    qualifications = binding.qualifications.text.toString().trim(),
                    role = binding.role.text.toString().trim(),
                    isInternal = true
                )
                submitJobOffer(jobOfferRequest)
            }
        }

        binding.cancelButton.setOnClickListener { clearInputs() }
    }

    private fun validateInputs(): Boolean {
        val jobTitle = binding.jobTitle.text.toString().trim()
        val description = binding.description.text.toString().trim()
        val department = binding.departmentSpinner.selectedItem.toString()
        val responsibilities = binding.responsibilities.text.toString().trim()
        val qualifications = binding.qualifications.text.toString().trim()
        val role = binding.role.text.toString().trim()

        if (jobTitle.isEmpty()) {
            binding.jobTitle.error = "Job Title is required"
            binding.jobTitle.requestFocus()
            return false
        }
        if (description.isEmpty()) {
            binding.description.error = "Description is required"
            binding.description.requestFocus()
            return false
        }
        if (department == "Select a department") {
            Toast.makeText(requireContext(), "Please select a department", Toast.LENGTH_SHORT).show()
            return false
        }
        if (responsibilities.isEmpty()) {
            binding.responsibilities.error = "Responsibilities are required"
            binding.responsibilities.requestFocus()
            return false
        }
        if (qualifications.isEmpty()) {
            binding.qualifications.error = "Qualifications are required"
            binding.qualifications.requestFocus()
            return false
        }
        if (role.isEmpty()) {
            binding.role.error = "Role is required"
            binding.role.requestFocus()
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitJobOffer(jobOffer: JobOfferRequest) {
        Log.d("TOKEN", "Current token = ${RetrofitClient.getAuthToken()}")

        val apiService = RetrofitClient.recruitingService

        apiService.createJobOffer(jobOffer).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Job offer saved successfully", Toast.LENGTH_SHORT).show()
                    clearInputs()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearInputs() {
        binding.jobTitle.text?.clear()
        binding.description.text?.clear()
        binding.departmentSpinner.setSelection(0)
        binding.responsibilities.text?.clear()
        binding.qualifications.text?.clear()
        binding.role.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
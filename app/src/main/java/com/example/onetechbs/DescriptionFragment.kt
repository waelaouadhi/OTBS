package com.example.onetechbs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class DescriptionFragment : Fragment() {
    
    private val viewModel: JobOfferCreationViewModel by activityViewModels()
    
    private lateinit var titleInput: TextInputEditText
    private lateinit var summaryInput: TextInputEditText
    private lateinit var departmentInput: TextInputEditText
    private lateinit var responsibilitiesInput: TextInputEditText
    private lateinit var requiredQualificationsInput: TextInputEditText
    private lateinit var preferredQualificationsInput: TextInputEditText
    private lateinit var whatWeOfferInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_job_description, container, false)
        
        initViews(view)
        setupObservers()
        setupClickListeners()
        
        return view
    }
    
    private fun initViews(view: View) {
        titleInput = view.findViewById(R.id.etTitle)
        summaryInput = view.findViewById(R.id.etSummary)
        departmentInput = view.findViewById(R.id.etDepartment)
        responsibilitiesInput = view.findViewById(R.id.etResponsibilities)
        requiredQualificationsInput = view.findViewById(R.id.etRequiredQualifications)
        preferredQualificationsInput = view.findViewById(R.id.etPreferredQualifications)
        whatWeOfferInput = view.findViewById(R.id.etWhatWeOffer)
        saveButton = view.findViewById(R.id.btnSaveAndProceed)
        // Toolbar back navigation
        view.findViewById<MaterialToolbar?>(R.id.toolbar)?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedTitle.collect { title ->
                if (title.isNotEmpty() && titleInput.text.toString() != title) {
                    titleInput.setText(title)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collect { summary ->
                if (summary.isNotEmpty() && summaryInput.text.toString() != summary) {
                    summaryInput.setText(summary)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedDepartment.collect { department ->
                if (department.isNotEmpty() && departmentInput.text.toString() != department) {
                    departmentInput.setText(department)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.responsibilities.collect { responsibilities ->
                val responsibilitiesText = responsibilities.joinToString("\n")
                if (responsibilitiesText.isNotEmpty() && responsibilitiesInput.text.toString() != responsibilitiesText) {
                    responsibilitiesInput.setText(responsibilitiesText)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.requiredQualifications.collect { qualifications ->
                val qualificationsText = qualifications.joinToString("\n")
                if (qualificationsText.isNotEmpty() && requiredQualificationsInput.text.toString() != qualificationsText) {
                    requiredQualificationsInput.setText(qualificationsText)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.preferredQualifications.collect { qualifications ->
                val qualificationsText = qualifications.joinToString("\n")
                if (qualificationsText.isNotEmpty() && preferredQualificationsInput.text.toString() != qualificationsText) {
                    preferredQualificationsInput.setText(qualificationsText)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.whatWeOffer.collect { offer ->
                if (offer.isNotEmpty() && whatWeOfferInput.text.toString() != offer) {
                    whatWeOfferInput.setText(offer)
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                saveData()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_layout, EvaluationFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        if (titleInput.text.toString().trim().isEmpty()) {
            titleInput.error = "Title is required"
            isValid = false
        }
        
        if (summaryInput.text.toString().trim().isEmpty()) {
            summaryInput.error = "Summary is required"
            isValid = false
        }
        
        if (departmentInput.text.toString().trim().isEmpty()) {
            departmentInput.error = "Department is required"
            isValid = false
        }
        
        if (responsibilitiesInput.text.toString().trim().isEmpty()) {
            responsibilitiesInput.error = "Responsibilities are required"
            isValid = false
        }
        
        if (requiredQualificationsInput.text.toString().trim().isEmpty()) {
            requiredQualificationsInput.error = "Required qualifications are required"
            isValid = false
        }
        
        if (whatWeOfferInput.text.toString().trim().isEmpty()) {
            whatWeOfferInput.error = "What we offer is required"
            isValid = false
        }
        
        if (!isValid) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
        }
        
        return isValid
    }
    
    private fun saveData() {
        viewModel.updateGeneratedTitle(titleInput.text.toString().trim())
        viewModel.updateSummary(summaryInput.text.toString().trim())
        viewModel.updateGeneratedDepartment(departmentInput.text.toString().trim())
        
        // Parse responsibilities (one per line)
        val responsibilities = responsibilitiesInput.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModel.updateResponsibilities(responsibilities)
        
        // Parse required qualifications (one per line)
        val requiredQualifications = requiredQualificationsInput.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModel.updateRequiredQualifications(requiredQualifications)
        
        // Parse preferred qualifications (one per line)
        val preferredQualifications = preferredQualificationsInput.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModel.updatePreferredQualifications(preferredQualifications)
        
        viewModel.updateWhatWeOffer(whatWeOfferInput.text.toString().trim())
    }
}

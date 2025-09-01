package com.example.onetechbs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.onetechbs.util.SharedPreferencesManager
import com.example.onetechbs.AIService

class DetailsFragment : Fragment() {
    
    private val viewModel: JobOfferCreationViewModel by activityViewModels()
    
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var departmentSpinner: Spinner
    private lateinit var experienceLevelSpinner: Spinner
    private lateinit var responsibilitiesInput: TextInputEditText
    private lateinit var mustHaveSkillsInput: TextInputEditText
    private lateinit var niceToHaveSkillsInput: TextInputEditText
    private lateinit var teamVibeInput: TextInputEditText
    private lateinit var generateButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_job_details, container, false)
        
        initViews(view)
        setupSpinners()
        setupServices()
        setupObservers()
        setupClickListeners()
        
        return view
    }
    
    private fun initViews(view: View) {
        jobTitleInput = view.findViewById(R.id.etJobTitle)
        departmentSpinner = view.findViewById(R.id.spinnerDepartment)
        experienceLevelSpinner = view.findViewById(R.id.spinnerExperienceLevel)
        responsibilitiesInput = view.findViewById(R.id.etResponsibilities)
        mustHaveSkillsInput = view.findViewById(R.id.etMustHaveSkills)
        niceToHaveSkillsInput = view.findViewById(R.id.etNiceToHaveSkills)
        teamVibeInput = view.findViewById(R.id.etTeamVibe)
        generateButton = view.findViewById(R.id.btnGenerateDescription)
        progressBar = view.findViewById(R.id.progressBar)
        // Toolbar back navigation
        view.findViewById<MaterialToolbar?>(R.id.toolbar)?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        // Disable until services are initialized
        generateButton.isEnabled = false
    }
    
    private fun setupSpinners() {
        // Department spinner
        val departments = arrayOf(
            "Select Department",
            "Engineering",
            "Marketing",
            "Sales",
            "Human Resources",
            "Finance",
            "Operations",
            "Customer Support",
            "Product Management",
            "Design",
            "Data Science"
        )
        val departmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, departments)
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        departmentSpinner.adapter = departmentAdapter
        
        // Experience level spinner
        val experienceLevels = arrayOf(
            "Select Experience Level",
            "Entry Level (0-2 years)",
            "Mid Level (2-5 years)",
            "Senior Level (5-8 years)",
            "Lead Level (8-12 years)",
            "Executive Level (12+ years)"
        )
        val experienceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, experienceLevels)
        experienceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        experienceLevelSpinner.adapter = experienceAdapter
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                generateButton.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
        
        // Debug: log generatedTitle to verify it gets populated
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedTitle.collect { title ->
                android.util.Log.d("DetailsFragment", "generatedTitle='$title'")
            }
        }
        
        // Navigate to description fragment after successful generation (when loading finished)
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.generatedTitle,
                viewModel.isLoading,
                viewModel.errorMessage
            ) { title, loading, error -> Triple(title, loading, error) }
                .collect { (title, loading, error) ->
                    if (title.isNotBlank() && !loading && error == null) {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_layout, DescriptionFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                }
        }
    }
    
    private fun setupClickListeners() {
        generateButton.setOnClickListener {
            if (validateInputs()) {
                collectInputData()
                viewModel.generateJobDescription()
            }
        }
        
        departmentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    viewModel.updateDepartment(departmentSpinner.selectedItem.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        experienceLevelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    viewModel.updateExperienceLevel(experienceLevelSpinner.selectedItem.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        if (jobTitleInput.text.toString().trim().isEmpty()) {
            jobTitleInput.error = "Job title is required"
            isValid = false
        }
        
        if (departmentSpinner.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Please select a department", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (experienceLevelSpinner.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Please select experience level", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (responsibilitiesInput.text.toString().trim().isEmpty()) {
            responsibilitiesInput.error = "Key responsibilities are required"
            isValid = false
        }
        
        if (mustHaveSkillsInput.text.toString().trim().isEmpty()) {
            mustHaveSkillsInput.error = "Must-have skills are required"
            isValid = false
        }
        
        if (teamVibeInput.text.toString().trim().isEmpty()) {
            teamVibeInput.error = "Team vibe description is required"
            isValid = false
        }
        
        return isValid
    }
    
    private fun collectInputData() {
        viewModel.updateJobTitle(jobTitleInput.text.toString().trim())
        
        // Parse responsibilities (one per line)
        val responsibilities = responsibilitiesInput.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModel.updateKeyResponsibilities(responsibilities)
        
        // Parse must-have skills (comma-separated)
        val mustHaveSkills = mustHaveSkillsInput.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModel.updateMustHaveSkills(mustHaveSkills)
        
        // Parse nice-to-have skills (comma-separated)
        val niceToHaveSkills = niceToHaveSkillsInput.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModel.updateNiceToHaveSkills(niceToHaveSkills)
        
        viewModel.updateTeamVibe(teamVibeInput.text.toString().trim())
    }

    private fun setupServices() {
        // Build authenticated OkHttp client
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing auth token. Please log in.", Toast.LENGTH_LONG).show()
            return
        }
        // Provide token to ViewModel for header-based calls
        viewModel.setAuthToken(token)
        val authInterceptor = Interceptor { chain ->
            val reqBuilder = chain.request().newBuilder()
            // Authorization header is passed via Retrofit @Header in ViewModel. Do not duplicate here.
            // be explicit about accepted response type
            reqBuilder.addHeader("Accept", "application/json")
            chain.proceed(reqBuilder.build())
        }
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        // Retrofit for AI service
        val retrofit = Retrofit.Builder()
            .baseUrl("http://172.31.4.101:5001/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val aiService = retrofit.create(AIService::class.java)
        viewModel.setAIService(aiService)
        // Enable after init
        generateButton.isEnabled = true
    }
}

package com.example.onetechbs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onetechbs.db.*
import com.example.onetechbs.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

class JobOfferCreationViewModel : ViewModel() {
    
    // Job Details (Phase 1)
    private val _jobTitle = MutableStateFlow("")
    val jobTitle: StateFlow<String> = _jobTitle.asStateFlow()
    
    private val _department = MutableStateFlow("")
    val department: StateFlow<String> = _department.asStateFlow()
    
    private val _experienceLevel = MutableStateFlow("")
    val experienceLevel: StateFlow<String> = _experienceLevel.asStateFlow()
    
    private val _keyResponsibilities = MutableStateFlow<List<String>>(emptyList())
    val keyResponsibilities: StateFlow<List<String>> = _keyResponsibilities.asStateFlow()
    
    private val _mustHaveSkills = MutableStateFlow<List<String>>(emptyList())
    val mustHaveSkills: StateFlow<List<String>> = _mustHaveSkills.asStateFlow()
    
    private val _niceToHaveSkills = MutableStateFlow<List<String>>(emptyList())
    val niceToHaveSkills: StateFlow<List<String>> = _niceToHaveSkills.asStateFlow()
    
    private val _teamVibe = MutableStateFlow("")
    val teamVibe: StateFlow<String> = _teamVibe.asStateFlow()
    
    // Job Description (Phase 2)
    private val _generatedTitle = MutableStateFlow("")
    val generatedTitle: StateFlow<String> = _generatedTitle.asStateFlow()
    
    private val _summary = MutableStateFlow("")
    val summary: StateFlow<String> = _summary.asStateFlow()
    
    private val _generatedDepartment = MutableStateFlow("")
    val generatedDepartment: StateFlow<String> = _generatedDepartment.asStateFlow()
    
    private val _responsibilities = MutableStateFlow<List<String>>(emptyList())
    val responsibilities: StateFlow<List<String>> = _responsibilities.asStateFlow()
    
    private val _requiredQualifications = MutableStateFlow<List<String>>(emptyList())
    val requiredQualifications: StateFlow<List<String>> = _requiredQualifications.asStateFlow()
    
    private val _preferredQualifications = MutableStateFlow<List<String>>(emptyList())
    val preferredQualifications: StateFlow<List<String>> = _preferredQualifications.asStateFlow()
    
    private val _whatWeOffer = MutableStateFlow("")
    val whatWeOffer: StateFlow<String> = _whatWeOffer.asStateFlow()
    
    // Evaluation Criteria (Phase 3)
    private val _criteria = MutableStateFlow<List<CriterionDTO>>(
        listOf(
            CriterionDTO("Skills Match", 25),
            CriterionDTO("Relevant Experience", 20),
            CriterionDTO("Education", 15),
            CriterionDTO("Certifications", 10),
            CriterionDTO("Cultural Fit", 15),
            CriterionDTO("Language Proficiency", 10),
            CriterionDTO("Achievements/Projects", 5)
        )
    )
    val criteria: StateFlow<List<CriterionDTO>> = _criteria.asStateFlow()
    
    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isSubmissionSuccessful = MutableStateFlow(false)
    val isSubmissionSuccessful: StateFlow<Boolean> = _isSubmissionSuccessful.asStateFlow()
    
    // AI Service for job description generation
    private lateinit var aiService: AIService
    
    // Backend API Service
    private lateinit var apiService: ApiService
    
    // Authorization token for secured endpoints
    private var authToken: String? = null

    fun setApiService(service: ApiService) {
        apiService = service
    }
    
    fun setAIService(service: AIService) {
        aiService = service
    }
    
    fun setAuthToken(token: String) {
        authToken = token
    }
    
    // Phase 1: Update job details
    fun updateJobTitle(title: String) {
        _jobTitle.value = title
    }
    
    fun updateDepartment(dept: String) {
        _department.value = dept
    }
    
    fun updateExperienceLevel(level: String) {
        _experienceLevel.value = level
    }
    
    fun updateKeyResponsibilities(responsibilities: List<String>) {
        _keyResponsibilities.value = responsibilities
    }
    
    fun updateMustHaveSkills(skills: List<String>) {
        _mustHaveSkills.value = skills
    }
    
    fun updateNiceToHaveSkills(skills: List<String>) {
        _niceToHaveSkills.value = skills
    }
    
    fun updateTeamVibe(vibe: String) {
        _teamVibe.value = vibe
    }
    
    // Phase 1: Generate job description
    fun generateJobDescription() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Check if aiService is initialized
                if (!::aiService.isInitialized) {
                    _errorMessage.value = "AI service not initialized. Please try again."
                    return@launch
                }
                
                // Check for authorization token
                if (authToken.isNullOrBlank()) {
                    _errorMessage.value = "Authorization token is missing. Please sign in again."
                    return@launch
                }
                
                val request = JobDetailsRequest(
                    jobTitle = _jobTitle.value,
                    department = _department.value,
                    experienceLevel = _experienceLevel.value,
                    keyResponsibilities = _keyResponsibilities.value,
                    mustHaveSkills = _mustHaveSkills.value,
                    niceToHaveSkills = _niceToHaveSkills.value,
                    teamVibe = _teamVibe.value
                )
                
                val response = aiService.generateJobDescription(
                    "Bearer ${authToken!!}",
                    request
                )
                if (response.isSuccessful) {
                    response.body()?.let { jobDesc ->
                        _generatedTitle.value = jobDesc.title
                        _summary.value = jobDesc.summary
                        _generatedDepartment.value = jobDesc.department ?: _department.value
                        _responsibilities.value = jobDesc.responsibilities
                        _requiredQualifications.value = jobDesc.qualifications_required
                        _preferredQualifications.value = jobDesc.qualifications_preferred
                        _whatWeOffer.value = jobDesc.what_we_offer
                    }
                } else {
                    val err = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    _errorMessage.value = "Failed to generate job description: ${response.code()} ${response.message()}${if (!err.isNullOrBlank()) " - $err" else ""}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error generating job description: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Phase 2: Update generated description fields
    fun updateGeneratedTitle(title: String) {
        _generatedTitle.value = title
    }
    
    fun updateSummary(summary: String) {
        _summary.value = summary
    }
    
    fun updateGeneratedDepartment(dept: String) {
        _generatedDepartment.value = dept
    }
    
    fun updateResponsibilities(responsibilities: List<String>) {
        _responsibilities.value = responsibilities
    }
    
    fun updateRequiredQualifications(qualifications: List<String>) {
        _requiredQualifications.value = qualifications
    }
    
    fun updatePreferredQualifications(qualifications: List<String>) {
        _preferredQualifications.value = qualifications
    }
    
    fun updateWhatWeOffer(offer: String) {
        _whatWeOffer.value = offer
    }
    
    // Phase 3: Update evaluation criteria
    fun updateCriteria(newCriteria: List<CriterionDTO>) {
        _criteria.value = newCriteria
    }
    
    fun updateCriterionWeight(index: Int, weight: Int) {
        val updatedCriteria = _criteria.value.toMutableList()
        if (index < updatedCriteria.size) {
            updatedCriteria[index] = updatedCriteria[index].copy(weight = weight)
            _criteria.value = updatedCriteria
        }
    }
    
    // Validation functions
    fun validatePhase1(): Boolean {
        return _jobTitle.value.isNotBlank() &&
                _department.value.isNotBlank() &&
                _experienceLevel.value.isNotBlank() &&
                _keyResponsibilities.value.isNotEmpty() &&
                _mustHaveSkills.value.isNotEmpty() &&
                _teamVibe.value.isNotBlank()
    }
    
    fun validatePhase2(): Boolean {
        return _generatedTitle.value.isNotBlank() &&
                _summary.value.isNotBlank() &&
                _generatedDepartment.value.isNotBlank() &&
                _responsibilities.value.isNotEmpty() &&
                _requiredQualifications.value.isNotEmpty() &&
                _whatWeOffer.value.isNotBlank()
    }
    
    fun validatePhase3(): Boolean {
        val totalWeight = _criteria.value.sumOf { it.weight }
        return totalWeight == 100 && _criteria.value.all { it.weight >= 0 }
    }
    
    // Phase 4: Submit job offer
    fun submitJobOffer() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val jobOfferRequest = JobOfferRequestDTO(
                    title = _generatedTitle.value,
                    summary = _summary.value,
                    responsibilities = _responsibilities.value,
                    qualifications_required = _requiredQualifications.value,
                    qualifications_preferred = _preferredQualifications.value,
                    what_we_offer = _whatWeOffer.value,
                    department = _generatedDepartment.value,
                    isInternal = true,
                    criteria = _criteria.value
                )
                
                // Debug request summary (avoid logging all content if large)
                Log.d(
                    Companion.TAG_JOB_VM,
                    "Submitting job offer: title='${jobOfferRequest.title}', dept='${jobOfferRequest.department}', resp=${jobOfferRequest.responsibilities.size}, reqQual=${jobOfferRequest.qualifications_required.size}, prefQual=${jobOfferRequest.qualifications_preferred.size}, criteria=${jobOfferRequest.criteria.size}"
                )
                
                val response = apiService.createJobOfferNew(jobOfferRequest)
                if (response.isSuccessful) {
                    Log.i(Companion.TAG_JOB_VM, "Job offer created successfully (HTTP ${response.code()})")
                    _isSubmissionSuccessful.value = true
                } else {
                    val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    val msg = "Failed to create job offer: ${response.code()} ${response.message()}${if (!errBody.isNullOrBlank()) " - $errBody" else ""}"
                    Log.e(Companion.TAG_JOB_VM, msg)
                    _errorMessage.value = msg
                }
            } catch (e: Exception) {
                Log.e(Companion.TAG_JOB_VM, "Exception while creating job offer", e)
                _errorMessage.value = "Error creating job offer: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun resetSubmissionState() {
        _isSubmissionSuccessful.value = false
    }

    companion object {
        private const val TAG_JOB_VM = "JobOfferCreationVM"
    }
}

// AI Service interface
interface AIService {
    @retrofit2.http.POST("api/v1/generate-job-description")
    suspend fun generateJobDescription(
        @retrofit2.http.Header("Authorization") token: String,
        @retrofit2.http.Body request: JobDetailsRequest
    ): retrofit2.Response<JobDescriptionResponse>
}

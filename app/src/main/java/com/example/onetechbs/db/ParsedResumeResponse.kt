package com.example.onetechbs.db

import com.google.gson.annotations.SerializedName

// Top-level response from AI parse-resume endpoint
data class ParsedResumeResponse(
    @SerializedName("candidate_info") val candidateInfo: AiCandidateInfoDTO?,
    val certifications: List<AiCertificationDTO> = emptyList(),
    val education: List<AiEducationDTO> = emptyList(),
    val experience: List<AiExperienceDTO> = emptyList(),
    val languages: List<AiLanguageDTO> = emptyList(),
    val projects: List<AiProjectDTO> = emptyList(),
    val skills: AiSkillsDTO? = null
)

data class AiCandidateInfoDTO(
    val email: String?,
    val linkedin: String?,
    val location: AiLocationDTO?,
    val name: String?,
    val phone: String?,
    val website: String?
)

data class AiLocationDTO(
    val city: String?,
    val country: String?,
    val state: String?
)

data class AiCertificationDTO(
    val name: String?,
    val date: String? = null,
    val expires: String? = null,
    val issuer: String? = null
)

data class AiEducationDTO(
    val degree: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("field_of_study") val fieldOfStudy: String?,
    val institution: String?,
    val location: String?,
    @SerializedName("start_date") val startDate: String?
)

data class AiExperienceDTO(
    val achievements: List<String> = emptyList(),
    val company: String?,
    @SerializedName("end_date") val endDate: String?,
    val location: String?,
    val responsibilities: List<String> = emptyList(),
    @SerializedName("start_date") val startDate: String?,
    val title: String?
)

data class AiLanguageDTO(
    val language: String?,
    val proficiency: String?
)

data class AiProjectDTO(
    val name: String?,
    val description: String? = null,
    val technologies: List<String> = emptyList(),
    val url: String? = null
)

data class AiSkillsDTO(
    val soft: List<String> = emptyList(),
    val technical: List<String> = emptyList()
)

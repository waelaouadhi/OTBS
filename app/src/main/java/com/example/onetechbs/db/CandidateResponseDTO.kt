package com.example.onetechbs.db



data class CandidateResponseDTO(
    val id: Long,
    val candidateInfo: CandidateInfoDTO,
    val certifications: List<CertificationDTO>,
    val education: List<EducationDTO>,
    val experience: List<ExperienceDTO>,
    val languages: List<LanguageDTO>,
    val projects: List<ProjectDTO>,
    val skills: SkillsDTO,
    val createdAt: String,   // or LocalDateTime if you set up a converter
    val updatedAt: String    // or LocalDateTime if you set up a converter
)
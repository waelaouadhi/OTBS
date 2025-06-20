package com.example.onetechbs.db

data class CandidateRequestDTO(
    val candidateInfo: CandidateInfo,
    val certifications: List<Certification>,
    val education: List<Education>,
    val experience: List<Experience>,
    val languages: List<Language>,
    val projects: List<Project>,
    val skills: Skills
)
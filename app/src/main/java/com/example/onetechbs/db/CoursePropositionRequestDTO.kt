package com.example.onetechbs.db

data class CoursePropositionRequestDTO(
    val title: String,
    val description: String,
    val justification: String,
    val expectedOutcomes: String,
    val isCertified: Boolean,
    val preferredProvider: String
)

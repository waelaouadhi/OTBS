package com.example.onetechbs.db

data class ExperienceDTO(
    val id: Long,
    val achievements: List<String>,
    val company: String,
    val endDate: String,
    val location: String,
    val responsibilities: List<String>,
    val startDate: String,
    val title: String
)
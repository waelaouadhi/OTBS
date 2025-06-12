package com.example.onetechbs.db

data class EducationDTO(
    val id: Long,
    val degree: String,
    val endDate: String,
    val fieldOfStudy: String,
    val institution: String,
    val location: String,
    val startDate: String
)
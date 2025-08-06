package com.example.onetechbs.db

import java.time.LocalDateTime

/**
 * Data class representing a course proposition as returned by the backend.
 */
data class CoursePropositionResponseDTO(
    val id: Long,
    val title: String,
    val managerFullName: String,
    val department: String,
    val description: String,
    val justification: String,
    val expectedOutcomes: String,
    @com.google.gson.annotations.SerializedName("certified")
    val isCertified: Boolean,
    val preferredProvider: String,
    val status: ECoursePropositionStatus,
    val createdAt: LocalDateTime
)

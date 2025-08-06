package com.example.onetechbs.db

import java.time.Instant

// Nested CourseResponseDTO is already defined in db

data class TrainingRequestResponseDTO(
    val id: Long,
    val course: CourseResponseDTO?,
    val employeeId: String?,
    val employeeFullName: String?,
    val status: String?, // Use String for RequestStatus (APPROVED, PENDING, REJECTED)
    val requestDate: Instant?,
    val reviewDate: Instant?,
    val rejectionReason: String?
)

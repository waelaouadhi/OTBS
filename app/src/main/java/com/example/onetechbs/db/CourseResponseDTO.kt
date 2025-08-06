package com.example.onetechbs.db

import java.math.BigDecimal

data class CourseResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val hasCertification: Boolean,
    val cost: BigDecimal?,
    val isEnrolled: Boolean,
    val isRequested: Boolean
)

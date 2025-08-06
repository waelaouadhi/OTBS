package com.example.onetechbs.db

import java.math.BigDecimal

data class CourseRequestDTO(
    val title: String,
    val description: String,
    val hasCertification: Boolean,
    val cost: BigDecimal
)

package com.example.onetechbs.db

import java.time.LocalDate
import java.time.LocalTime

data class Leave(
    val id: Long? = null,
    val userDn: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val leaveType: ELeaveType,
    val status: EStatus,
    val attachment: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
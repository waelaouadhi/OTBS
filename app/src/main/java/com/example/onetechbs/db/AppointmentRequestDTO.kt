package com.example.onetechbs.db

import java.time.LocalDateTime

data class AppointmentRequestDTO(
    val medicalVisitId: Long,
    val timeSlot: LocalDateTime,
    val employeeId: String
) 
package com.example.onetechbs.db

import java.time.LocalDateTime

data class AppointmentResponseDTO(
    val id: Long,
    val medicalVisitId: Long,
    val doctorName: String,
    val timeSlot: LocalDateTime,
    val status: EAppointmentStatus,
    val employeeFullName: String,
    val employeeEmail: String
)


package com.example.onetechbs.db

import java.time.LocalDate
import java.time.LocalTime

data class MedicalVisitResponse(
    val id: Long,
    val doctorName: String,
    val visitDate: String, // Use String if it's a date string
    val startTime: String,
    val endTime: String,
    val numberOfAppointments: Int
)
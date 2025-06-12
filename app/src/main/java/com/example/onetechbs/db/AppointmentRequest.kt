package com.example.onetechbs.db

data class AppointmentRequest(
    val medicalVisitId: Long,
    val timeSlot: String // ISO 8601 format, e.g., "2025-06-20T10:00:00"
)
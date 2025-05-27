package com.example.onetechbs.db

data class MedicalVisitRequest(
    val doctorName: String,
    val visitDate: String,      // Format: "YYYY-MM-DD"
    val startTime: String,      // Format: "HH:mm:ss"
    val endTime: String         // Format: "HH:mm:ss"
)
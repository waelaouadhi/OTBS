package com.example.onetechbs.db

import java.time.LocalDateTime

data class MedicalVisitNotification(
    val id: Long,
    val visitTitle: String,
    val patientName: String,
    val visitDate: LocalDateTime,
    val doctorName: String,
    val type: String

)
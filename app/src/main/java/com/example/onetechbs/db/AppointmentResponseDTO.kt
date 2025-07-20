package com.example.onetechbs.db

import java.time.LocalDateTime

import com.google.gson.annotations.SerializedName

data class AppointmentResponseDTO(
    val id: Long,
    val medicalVisitId: Long,
    val doctorName: String,
    val timeSlot: LocalDateTime,
    val status: EAppointmentStatus?,
    @SerializedName("userFullName")
    val employeeFullName: String?,
    @SerializedName("userEmail")
    val employeeEmail: String?
)


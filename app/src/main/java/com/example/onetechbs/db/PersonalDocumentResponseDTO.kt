package com.example.onetechbs.db


import java.time.LocalDate

data class PersonalDocumentResponseDTO(
    val id: Long,
    val employeeName: String,
    val documentType: EDocumentType,
    val requestDate: String,
    val status: EDocumentStatus,
    val notes: String,
    val document: String
)
package com.example.onetechbs.db


data class PersonalDocumentRequestDTO(
    val documentType: EDocumentType,
    val notes: String
)
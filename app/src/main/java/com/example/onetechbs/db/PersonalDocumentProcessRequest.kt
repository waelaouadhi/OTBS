package com.example.onetechbs.db

data class PersonalDocumentProcessRequest(
    val status: EDocumentStatus,
    val notes: String
)
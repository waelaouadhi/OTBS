package com.example.onetechbs.db

data class InternalApplicationDTO(
    val id: Int,
    val fullName: String,
    val phoneNumber: String,
    val cvLink: String,
    val email: String,
    val status: String,
    val submissionDate: String
)
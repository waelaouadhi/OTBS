package com.example.onetechbs.db

data class JobOfferRequest(
    val title: String,
    val department: String,
    val description: String,
    val responsibilities: String,
val qualifications: String,
val role: String,
val isInternal: Boolean
)
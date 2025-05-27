package com.example.onetechbs.db

data class EmployeeResponse(
    val id: String,
    val username: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val jobTitle: String
    // Add other fields from your backend as neede
)
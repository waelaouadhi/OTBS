package com.example.onetechbs.db

data class InvitationResponse(
    val id: Long,
    val employeeName: String,
    val status: String,  // PENDING, ACCEPTED, REJECTED, etc.
    val employeeId: String
)
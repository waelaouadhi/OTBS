package com.example.onetechbs.db

data class InvitationResponseDTO(
    val id: Long,
    val employeeName: String,
    val status: EStatus,
    val employeeId: String
)
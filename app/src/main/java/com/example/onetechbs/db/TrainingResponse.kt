package com.example.onetechbs.db

data class TrainingResponse(
    val id: Long,
    val title: String,
    val description: String,
    val department: String,
    val startDate: String,
    val endDate: String,
    val createdBy: String,
    val invitations: List<InvitationResponseDTO>,
    val createdAt: String
)
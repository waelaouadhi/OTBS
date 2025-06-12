package com.example.onetechbs.db

data class TrainingResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val department: String,
    val startDate: String,  // use String or LocalDate if using a converter
    val endDate: String,
    val createdBy: String,
    val invitations: List<InvitationResponseDTO>,
    val createdAt: String
)

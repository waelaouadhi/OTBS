package com.example.onetechbs.db

data class CandidateInfoDTO(
    val id: Long,
    val email: String,
    val linkedin: String,
    val location: LocationDTO,
    val name: String,
    val phone: String,
    val website: String
)
package com.example.onetechbs.db

data class ApplicationDetailsResponseDTO(
    val applicationId: Long,
    val resume: CandidateResponseDTO,
    val matchResult: MatchResult,
    val attachment: ByteArray
)
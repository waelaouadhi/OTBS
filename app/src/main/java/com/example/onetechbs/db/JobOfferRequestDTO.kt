package com.example.onetechbs.db

data class JobOfferRequestDTO(
    val title: String,
    val summary: String,
    val responsibilities: List<String>,
    val qualifications_required: List<String>,
    val qualifications_preferred: List<String>,
    val what_we_offer: String,
    val department: String,
    val isInternal: Boolean,
    val criteria: List<CriterionDTO>
)

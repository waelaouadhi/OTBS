package com.example.onetechbs.db

data class JobDescriptionResponse(
    val title: String,
    val summary: String,
    val responsibilities: List<String>,
    val qualifications_required: List<String>,
    val qualifications_preferred: List<String>,
    val what_we_offer: String,
    val department: String? = null
)

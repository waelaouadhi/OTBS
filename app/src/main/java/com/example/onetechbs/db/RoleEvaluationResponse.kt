package com.example.onetechbs.db

data class RoleEvaluationResponse(
    val roleType: String,
    val confidence: Double,
    val justification: String,
    val criteria: List<String>
) 
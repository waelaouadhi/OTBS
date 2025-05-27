package com.example.onetechbs.db

data class TrainingRequest(
    val title: String,
    val description: String,
    val department: String,
    val startDate: String, // Format: "yyyy-MM-dd"
    val endDate: String,   // Format: "yyyy-MM-dd"
    val createdBy: String
)
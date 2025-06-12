package com.example.onetechbs.db

data class JobDescription(
    val title: String,
    val company: String,
    val description: String,
    val requirements: List<String>,
    val responsibilities: List<String>,
    val location: Location,
    val salary: String
) 
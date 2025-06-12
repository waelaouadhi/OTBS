package com.example.onetechbs.db

data class Experience(
    val company: String,
    val title: String,
    val location: Location,
    val dates: String,
    val responsibilities: List<String>,
    val achievements: List<String>
) 
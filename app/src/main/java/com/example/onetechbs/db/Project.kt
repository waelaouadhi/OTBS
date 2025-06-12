package com.example.onetechbs.db

data class Project(
    val name: String,
    val description: String,
    val technologies: List<String>,
    val url: String
) 
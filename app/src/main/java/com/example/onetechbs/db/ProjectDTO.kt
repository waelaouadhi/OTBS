package com.example.onetechbs.db

data class ProjectDTO(
    val id: Long,
    val name: String,
    val description: String,
    val technologies: List<String>,
    val url: String
)
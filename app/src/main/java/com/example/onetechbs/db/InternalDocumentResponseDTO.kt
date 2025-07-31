package com.example.onetechbs.db

data class InternalDocumentResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val category: String,
    val document: String // base64 string
)
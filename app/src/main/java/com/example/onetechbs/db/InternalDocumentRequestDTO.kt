package com.example.onetechbs.db
import okhttp3.MultipartBody

data class InternalDocumentRequestDTO(
    val title: String,
    val description: String,
    val category: EDocumentCategory
)
package com.example.onetechbs.db

data class LeavePageResponse(
    val content: List<Leave>,
    val totalElements: Int,
    val totalPages: Int,
    val number: Int,
    val size: Int
)
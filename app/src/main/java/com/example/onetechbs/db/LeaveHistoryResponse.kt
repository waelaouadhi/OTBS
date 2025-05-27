package com.example.onetechbs.db

data class LeaveHistoryResponse(
    val id: Long,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val status: String
)
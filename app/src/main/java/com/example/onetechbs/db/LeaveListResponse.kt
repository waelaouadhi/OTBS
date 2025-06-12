package com.example.onetechbs.db

import com.example.onetechbs.db.LeaveResponse

data class LeaveListResponse(
    val status: String,
    val data: List<LeaveResponse>
)
package com.example.onetechbs.db
data class LeaveRequest(
    val leaveType: String,       // Must match ELeaveType exactly
    val startDate: String,       // Format: yyyy-MM-dd
    val endDate: String,         // Format: yyyy-MM-dd
    val status: String = "EN_ATTENTE" // Optional, default if needed
)
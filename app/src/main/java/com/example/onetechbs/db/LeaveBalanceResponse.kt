package com.example.onetechbs.db

data class LeaveBalanceResponse(
    val totalLeave: Int,
    val usedLeave: Int,
    val remainingLeave: Int
)
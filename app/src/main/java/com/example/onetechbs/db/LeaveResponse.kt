package com.example.onetechbs.db

import com.example.onetechbs.db.LeaveStatus
import java.time.LocalDate
import com.example.onetechbs.db.ELeaveType

 class LeaveResponse(
    val id: Long,
    val name: String,
    val department: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val leaveType: ELeaveType,
    val status: LeaveStatus
)
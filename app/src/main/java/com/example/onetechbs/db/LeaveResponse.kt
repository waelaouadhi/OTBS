package com.example.onetechbs.db

import com.example.onetechbs.db.LeaveStatus
import com.example.onetechbs.db.ELeaveType

 import com.google.gson.annotations.SerializedName
import java.time.LocalDate

class LeaveResponse(
    val id: Long,
    @SerializedName("attachment") val attachment: String? = null,
    @SerializedName("Name") val name: String,
    @SerializedName("Department") val department: String,
    @SerializedName("startDate") val startDate: LocalDate,
    @SerializedName("endDate") val endDate: LocalDate,
    @SerializedName("leaveType") val leaveType: ELeaveType,
    @SerializedName("status") val status: EStatus
)
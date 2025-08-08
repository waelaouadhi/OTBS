package com.example.onetechbs.attendance

data class AttendanceRecordResponseDTO(
    val employeeId: String,
    val employeeName: String,
    val department: String,
    val date: String, // yyyy-MM-dd
    val status: String, // EStatus
    val firstPunch: String?, // HH:mm
    val lastPunch: String?,  // HH:mm
    val totalHours: String,
    val allPunches: List<String>,
    val punchCount: Int,
    val issues: List<String>
)

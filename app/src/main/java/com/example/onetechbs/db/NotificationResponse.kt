package com.example.onetechbs.db

data class NotificationResponse(
    val id: String,
    val title: String?,
    val message: String?,
    val sender: String?,
    val recipient: String?,
    val type: String?,
    val createdAt: String?,
    val read: Boolean ,
    val sourceId: String?,
    val actionUrl: String?,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val department: String?,
    val createdBy: String?,
    val invitations: List<String>? = null
)
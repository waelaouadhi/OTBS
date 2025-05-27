package com.example.onetechbs.db

data class NotificationRequest(
    val read: Boolean = true,
    val recipient: String,
    val sender: String,
    val type: String,
    val message: String,
    val actionUrl: String,
    val title: String
)
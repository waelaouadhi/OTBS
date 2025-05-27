package com.example.onetechbs.db

data class Notification(
    val id: Long,
    val title: String,
    val message: String,
    val sender: String,
    val type: String,
    val createdAt: String,
    val read: Boolean,
)
package com.example.onetechbs.db

import android.app.Notification
import java.time.LocalDateTime

data class OtherNotification(
    val id: Long,
  val title: String,
    val message: String,
    val sender: String,
    val type: String,  // TRAINING, SYSTEM_ANNOUNCEMENT, PERSONAL, etc.
     val createdAt: LocalDateTime,
     val read: Boolean,
     val sourceId: String?,
     val actionUrl: String?
)
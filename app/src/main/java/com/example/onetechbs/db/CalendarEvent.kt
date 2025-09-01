package com.example.onetechbs.db

import java.time.LocalDate

enum class EventType { VACATION, SICK, DOCTOR, TRAINING, HOLIDAY }
enum class EventStatus { APPROVED, PENDING, REJECTED }

data class CalendarEvent(
    val date: LocalDate,
    val type: EventType,
    val status: EventStatus,
    val title: String = "",
    val description: String = ""
)

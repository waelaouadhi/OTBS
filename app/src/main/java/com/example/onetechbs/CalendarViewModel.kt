package com.example.onetechbs

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.onetechbs.db.CalendarEvent

class CalendarViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()

    fun setEvents(newEvents: List<CalendarEvent>) {
        _events.value = newEvents
    }
}

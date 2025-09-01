package com.example.onetechbs

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.onetechbs.db.*
import com.example.onetechbs.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarEventService(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchAllCalendarEvents(): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        try {
            // Fetch doctor appointments
            val doctorEvents = fetchDoctorAppointments()
            events.addAll(doctorEvents)
            
            // Fetch approved leaves
            val leaveEvents = fetchApprovedLeaves()
            events.addAll(leaveEvents)
            
        } catch (e: Exception) {
            Log.e("CalendarEventService", "Error fetching calendar events", e)
        }
        
        return events
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchDoctorAppointments(): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.medService.getDoctorVisits()
                if (response.isSuccessful) {
                    response.body()?.mapNotNull { visit ->
                        try {
                            val visitDate = LocalDate.parse(visit.visitDate)
                            CalendarEvent(
                                date = visitDate,
                                type = EventType.DOCTOR,
                                status = EventStatus.APPROVED,
                                title = "Doctor Visit - ${visit.doctorName}",
                                description = "Appointment with ${visit.doctorName} from ${visit.startTime} to ${visit.endTime}"
                            )
                        } catch (e: Exception) {
                            Log.e("CalendarEventService", "Error parsing doctor visit date: ${visit.visitDate}", e)
                            null
                        }
                    } ?: emptyList()
                } else {
                    Log.e("CalendarEventService", "Failed to fetch doctor visits: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("CalendarEventService", "Error fetching doctor appointments", e)
                emptyList()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchApprovedLeaves(): List<CalendarEvent> {
        return withContext(Dispatchers.IO) {
            try {
                val token = RetrofitClient.getAuthToken()
                if (token.isNullOrEmpty()) {
                    Log.e("CalendarEventService", "No auth token available")
                    return@withContext emptyList<CalendarEvent>()
                }

                val response = RetrofitClient.leaveService.getLeaveHistory("Bearer $token")
                if (response.isSuccessful) {
                    response.body()?.flatMap { leave ->
                        // Only include approved leaves
                        if (leave.status == EStatus.APPROVED) {
                            generateLeaveEventRange(leave)
                        } else {
                            emptyList()
                        }
                    } ?: emptyList()
                } else {
                    Log.e("CalendarEventService", "Failed to fetch leaves: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("CalendarEventService", "Error fetching approved leaves", e)
                emptyList()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateLeaveEventRange(leave: Leave): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        try {
            val startDate = leave.startDate
            val endDate = leave.endDate
            
            // Generate events for each day in the leave range
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val eventType = when (leave.leaveType) {
                    ELeaveType.ANNUEL -> EventType.VACATION
                    ELeaveType.MALADIE -> EventType.SICK
                    ELeaveType.SANS_SOLDE -> EventType.VACATION // Map personal to vacation for now
                    ELeaveType.MATERNITÉ -> EventType.VACATION
                    ELeaveType.PATERNITÉ -> EventType.VACATION
                    else -> EventType.VACATION
                }
                
                events.add(
                    CalendarEvent(
                        date = currentDate,
                        type = eventType,
                        status = EventStatus.APPROVED,
                        title = "${leave.leaveType.name.lowercase().replaceFirstChar { it.uppercase() }} Leave",
                        description = "Leave from ${leave.startDate} to ${leave.endDate}"
                    )
                )
                
                currentDate = currentDate.plusDays(1)
            }
        } catch (e: Exception) {
            Log.e("CalendarEventService", "Error generating leave event range", e)
        }
        
        return events
    }
}

package com.example.onetechbs.attendance

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AttendanceApiService {
    @GET("api/v1/attendances/attendance-records")
    suspend fun getAllAttendanceRecords(@Query("date") date: String? = null): List<AttendanceRecordResponseDTO>

    @GET("api/v1/attendances/employees/{employeeId}/attendance-records")
    suspend fun getAttendanceRecordsForEmployee(@Path("employeeId") employeeId: String): List<AttendanceRecordResponseDTO>

    @GET("api/v1/attendances/employees/{employeeId}/attendance-records/{date}")
    suspend fun getAttendanceRecordForEmployeeByDate(
        @Path("employeeId") employeeId: String,
        @Path("date") date: String
    ): AttendanceRecordResponseDTO
}

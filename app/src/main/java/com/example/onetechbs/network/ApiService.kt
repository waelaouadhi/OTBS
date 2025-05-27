package com.example.onetechbs.network

import com.example.onetechbs.Leave
import com.example.onetechbs.db.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth API


    @POST("api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshRequest): Call<JwtResponse>

    // Employee API
    @GET("api/v1/employee/username")
    fun getEmployeeByUsername(@Query("username") username: String): Call<EmployeeResponse>

    @GET("api/v1/employee/role")
    fun getEmployeeRole(@Query("username") username: String): Call<String>


    @GET("/api/v1/leave/apply")
    fun getUserLeaves(): Call<List<Leave>>

    @Multipart
    @POST("api/v1/leave/apply")
    fun applyLeave(
        @Part("leaveType") leaveType: RequestBody,
        @Part("startDate") startDate: RequestBody,
        @Part("endDate") endDate: RequestBody,
        @Part("startHOURLY") startHOURLY: RequestBody?,
        @Part("endHOURLY") endHOURLY: RequestBody?,
        @Part attachment: MultipartBody.Part? = null
    ): Call<MessageResponse>




    @PUT("api/v1/leave/reject/{leaveId}")
    fun rejectLeave(@Path("leaveId") leaveId: Long): Call<MessageResponse>

    // Training API
    @POST("api/v1/trainings")
    fun createTraining(@Body trainingRequest: TrainingRequest): Call<Void>

    @GET("api/v1/trainings")
    fun getAllTrainings(): Call<List<TrainingResponse>>

    // Notification API
    @POST("/api/v1/notifications")
    suspend fun createNotification(@Body notification: NotificationRequest): Response<Unit>

    @GET("/api/v1/notifications")
    suspend fun getAllNotifications(): Response<List<NotificationResponse>>

    @PUT("/api/notifications/unread/{id}")
    suspend fun markNotificationAsUnread(@Path("id") id: Long): Response<Unit>

    @PUT("/api/notifications/action/{id}")
    suspend fun handleManagerAction(@Path("id") id: Long, @Query("action") action: String): Response<Unit>

    @GET("api/v1/notifications")
    suspend fun getUserNotifications(): Response<List<NotificationResponse>>

    @GET("api/v1/notifications/unread")
    suspend fun getUnreadNotifications(): Response<List<NotificationResponse>>

    @GET("api/v1/notifications/unread/count")
    suspend fun getUnreadCount(): Response<Long>

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Long): Response<NotificationResponse>

    @PUT("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<Void>

    // Medical Visit API
    @POST("api/v1/medical-visits")
    suspend fun submitMedicalVisit(@Body request: MedicalVisitRequest): Response<Unit>

    @GET("api/v1/medical-visits")
    suspend fun getDoctorVisits(): Response<List<MedicalVisitResponse>>
    @POST("api/v1/auth/login")
    fun login(@Body request: AuthRequest): Call<JwtResponse>

    @GET("api/v1/leave/balance")
    fun getLeaveBalance(): Call<LeaveBalanceResponse>

    @GET("/api/v1/leave/history")
    fun getLeaveHistory(): Call<List<LeaveHistoryResponse>>

    @Multipart
    @POST("api/v1/leave/apply")
    fun applyLeave(
        @Part("leaveType") leaveType: RequestBody,
        @Part("startDate") startDate: RequestBody,
        @Part("endDate") endDate: RequestBody,
        @Part attachment: MultipartBody.Part? = null
    ): Call<MessageResponse>

    @DELETE("api/v1/leave/cancel/{leaveId}")
    fun cancelLeave(@Path("leaveId") leaveId: Long): Call<MessageResponse>

    @PUT("api/v1/leave/approve/{leaveId}")
    fun approveLeave(@Path("leaveId") leaveId: Long): Call<MessageResponse>
}

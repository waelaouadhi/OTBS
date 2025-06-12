package com.example.onetechbs.network

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.onetechbs.db.LeaveResponse
import com.example.onetechbs.db.*
import com.example.onetechbs.db.Leave
import com.example.onetechbs.db.AppointmentRequestDTO
import com.example.onetechbs.db.AppointmentResponseDTO
import com.example.onetechbs.db.MessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth API
    @POST("/api/v1/auth/login")
    fun login(@Body authRequest: AuthRequest): Call<JwtResponse>


    @POST("api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshRequest): Call<JwtResponse>

    // Employee API
    @GET("api/v1/employee/username")
    fun getEmployeeByUsername(@Query("username") username: String): Call<EmployeeResponse>

    @GET("api/v1/employee/role")
    fun getEmployeeRole(@Query("username") username: String): Call<String>

    // Leave API
    @GET("api/v1/leave/all")
    suspend fun getAllLeaves(): Response<List<LeaveResponse>>

    @GET("/api/v1/leave/received")
    suspend fun getLeaves(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "startDate,desc",
        @Header("Authorization") token: String
    ): Response<LeavePageResponse>

    @GET("api/v1/leave/balance")
    fun getLeaveBalance(): Call<LeaveBalanceResponse>

    @POST("api/v1/leave/apply")
    suspend fun createLeave(@Body leave: LeaveResponse): Response<LeaveResponse>

    @PUT("api/v1/leave/{id}/status")
    suspend fun updateLeaveStatus(
        @Path("id") id: Long,
        @Body status: Map<String, String>
    ): Response<LeaveResponse>

    @GET("api/v1/leave/employee/{employeeId}")
    fun getEmployeeLeaves(@Path("employeeId") employeeId: String): Call<List<LeaveResponse>>

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

    @DELETE("api/v1/leave/cancel/{leaveId}")
    fun cancelLeave(@Path("leaveId") leaveId: Long): Call<MessageResponse>

    @PUT("api/v1/leave/approve/{leaveId}")
    fun approveLeave(
        @Path("leaveId") leaveId: Long,
        @Header("Authorization") token: String
    ): Call<MessageResponse>

    @PUT("api/v1/leave/reject/{leaveId}")
    fun rejectLeave(
        @Path("leaveId") leaveId: Long,
        @Header("Authorization") token: String
    ): Call<MessageResponse>


    @GET("api/v1/leave/history")
    fun getLeaveHistory(): Call<List<LeaveHistoryResponse>>

    // Training API
    @POST("api/v1/trainings")
    fun createTraining(@Body trainingRequest: TrainingRequest): Call<Void>

    @GET("api/v1/trainings")
    fun getAllTrainings(): Call<List<TrainingResponseDTO>>

    // Notification API
    @POST("api/v1/notifications")
    suspend fun createNotification(@Body notification: NotificationRequest): Response<Unit>
    @GET("api/v1/notifications")
    suspend fun getAllNotifications(): Response<List<NotificationResponse>>

    @PUT("api/v1/notifications/unread/{id}")
    suspend fun markNotificationAsUnread(@Path("id") id: Long): Response<Unit>
    @GET("api/v1/trainings")
    suspend fun getTrainings(
        @Header("Authorization") token: String
    ): Response<List<TrainingResponseDTO>>


    @PUT("api/v1/notifications/action/{id}")
    suspend fun handleManagerAction(@Path("id") id: Long, @Query("action") action: String): Response<Unit>

    @GET("api/v1/notifications/unread")
    suspend fun getUnreadNotifications(): Response<List<NotificationResponse>>

    @GET("api/v1/notifications/unread/count")
    suspend fun getUnreadCount(): Response<Long>

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Long): Response<NotificationResponse>

    @PUT("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<Void>
    @GET("api/v1/trainings/{id}")
    suspend fun getTrainingById(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<TrainingResponseDTO>




    @Multipart
    @POST("api/v1/internal-applications/job-offer/{jobOfferId}")
    suspend fun createApplication(
        @Path("jobOfferId") jobOfferId: Long,
        @Part resume: MultipartBody.Part,
        @Header("Authorization") authHeader: String
    ): Response<Void>

    // Medical Visit API
    @POST("api/v1/medical-visits")
    suspend fun submitMedicalVisit(request1: String, @Body request: MedicalVisitRequest): Response<Unit>

    @POST("api/v1/appointments")
    suspend fun submitAppointment(
        @Header("Authorization") authToken: String,
        @Body request: AppointmentRequest
    ): Response<Unit>

    @GET("api/v1/medical-visits")
    suspend fun getDoctorVisits(): Response<List<MedicalVisitResponse>>

    @GET("api/v1/medical-visits")
    suspend fun getMedicalVisits(): List<MedicalVisitResponse>

    @DELETE("api/v1/medical-visits/{id}")
    suspend fun deleteMedicalVisit(@Path("id") id: Long): Response<MessageResponseDTO>
    @GET("api/v1/job-offers")
    fun getAllJobOffers(): Call<List<JobOfferResponseDTO>>

    @PUT("api/v1/job-offers/{id}/status")
    fun toggleJobOfferStatus(
        @Path("id") id: String,
        @Query("status") status: String
    ): Call<Void>

    @DELETE("api/v1/job-offers/{id}")
    fun deleteJobOffer(@Path("id") id: String): Call<Void>

    @GET("api/v1/invitations")
    suspend fun getAllTrainingInvitations(): Response<List<InvitationResponseDTO>>
    @DELETE("api/v1/trainings/{id}")
    suspend fun deleteTraining(@Path("id") id: Long): Response<MessageResponseDTO>

    @GET("api/v1/appointments")
    suspend fun getAllAppointments(): Response<List<AppointmentResponseDTO>>

    @GET("api/v1/appointments/{id}")
    suspend fun getAppointmentById(
        @Path("id") id: Long
    ): Response<AppointmentResponseDTO>

    @GET("api/v1/appointments/employee/{employeeId}")
    suspend fun getAppointmentsByEmployeeId(
        @Path("employeeId") employeeId: String
    ): Response<List<AppointmentResponseDTO>>

    @GET("api/v1/appointments/medVisit/{medVisitId}")
    suspend fun getAppointmentsByMedVisitId(
        @Path("medVisitId") medVisitId: String
    ): Response<List<AppointmentResponseDTO>>

    @POST("api/v1/appointments")
    suspend fun createAppointment(
        @Body appointmentRequest: AppointmentRequestDTO
    ): Response<MessageResponse>

    @PUT("api/v1/appointments/{id}")
    suspend fun updateAppointment(
        @Path("id") id: Long,
        @Body appointmentRequest: AppointmentRequestDTO
    ): Response<MessageResponse>
    @POST("api/v1/job-offers")
    fun createJobOffer(@Body request: JobOfferRequest): Call<Void>
    @DELETE("api/v1/appointments/{id}")
    suspend fun deleteAppointment(
        @Path("id") id: Long
    ): Response<MessageResponse>

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun createWithToken(token: String): ApiService {
            return RetrofitClient.getClient(token).create(ApiService::class.java)
        }
    }

    @GET("api/v1/job-offers/{id}")
    fun getJobOfferById(
        @Path("id") id: Long
    ): Call<JobOfferRequest>

    @GET("/api/v1/candidates") // adjust path if needed
    suspend fun listCandidates(): Response<List<CandidateResponseDTO>>

    @PUT("api/v1/job-offers/{id}")
    fun updateJobOffer(
        @Path("id") id: String,
        @Body jobOfferRequestDTO: JobOfferRequest
    ): Call<Void>
}
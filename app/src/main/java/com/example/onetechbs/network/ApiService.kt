package com.example.onetechbs.network

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.onetechbs.db.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    /**
     * API Service interface for OneTechBS application.
     * Provides methods to interact with various endpoints for authentication,
     * employee management, leave management, training sessions, notifications,
     * job offers, candidates, and medical visits.
     */

    // ─────────────────────────────────────────────────────────────
    // 🔐 AUTHENTICATION
    // ─────────────────────────────────────────────────────────────
    @POST("/api/v1/auth/login")
    fun login(@Body authRequest: AuthRequest): Call<JwtResponse>

    @POST("api/v1/auth/refresh")
    fun refreshToken(@Body request: RefreshRequest): Call<JwtResponse>

    // ─────────────────────────────────────────────────────────────
    // 👤 EMPLOYEE
    // ─────────────────────────────────────────────────────────────
    @GET("api/v1/users/username")
    fun getEmployeeByUsername(@Query("username") username: String): Call<EmployeeResponse>

    @GET("api/v1/users/role")
    fun getEmployeeRole(@Query("username") username: String): Call<String>

    // ─────────────────────────────────────────────────────────────
    // 📅 LEAVE MANAGEMENT
    // ─────────────────────────────────────────────────────────────
    @GET("api/v1/leave/all")
    suspend fun getLeavesdd(): Response<List<LeaveResponse>>


    @GET("api/v1/leave/all")
    suspend fun getAllReceivedLeaves(
        @Header("Authorization") authToken: String
    ): Response<List<LeaveResponse>>


    @GET("api/v1/leave/all")
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

    @GET("/api/v1/leave/myLeaves")
    suspend fun getLeaveHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "startDate,asc"
    ): Response<List<Leave>>

    // ─────────────────────────────────────────────────────────────
    // 🏋️‍♂️ TRAININGS
    // ─────────────────────────────────────────────────────────────
    @POST("api/v1/trainings")
    fun createTraining(@Body trainingRequest: TrainingRequest): Call<Void>

    @GET("api/v1/trainings")
    fun getAllTrainings(): Call<List<TrainingResponseDTO>>

    @GET("api/v1/trainings")
    suspend fun getTrainings(
        @Header("Authorization") token: String
    ): Response<List<TrainingResponseDTO>>

    @GET("api/v1/trainings/{id}")
    suspend fun getTrainingById(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<TrainingResponseDTO>

    @DELETE("api/v1/trainings/{id}")
    suspend fun deleteTraining(@Path("id") id: Long): Response<MessageResponseDTO>

    @GET("api/v1/invitations")
    suspend fun getAllTrainingInvitations(): Response<List<InvitationResponseDTO>>

    // ─────────────────────────────────────────────────────────────
    // 🔔 NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────
    @POST("api/v1/notifications")
    suspend fun createNotification(@Body notification: NotificationRequest): Response<Unit>

    @GET("api/v1/notifications")
    suspend fun getAllNotifications(): Response<List<NotificationResponse>>

    @PUT("api/v1/notifications/unread/{id}")
    suspend fun markNotificationAsUnread(@Path("id") id: Long): Response<Unit>

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

    // ─────────────────────────────────────────────────────────────
    // 💼 JOB OFFERS
    // ─────────────────────────────────────────────────────────────
    @GET("api/v1/job-offers")
    fun getAllJobOffers(): Call<List<JobOfferResponseDTO>>

    @GET("api/v1/job-offers/{jobId}")
    fun getJobOfferById(@Path("jobId") id: Long): Call<JobOfferRequest>

    @POST("api/v1/job-offers")
    fun createJobOffer(@Body request: JobOfferRequest): Call<Void>

    @PUT("api/v1/job-offers/{id}")
    fun updateJobOffer(
        @Path("id") id: String,
        @Body jobOfferRequestDTO: JobOfferRequest
    ): Call<Void>

    @PUT("api/v1/job-offers/{id}/status")
    fun toggleJobOfferStatus(
        @Path("id") jobId: String,
        @Query("status") status: String,
        @Header("Authorization") token: String
    ): Call<Void>

    @DELETE("api/v1/job-offers/{id}")
    fun deleteJobOffer(@Path("id") id: String): Call<Void>

    // ─────────────────────────────────────────────────────────────
    // 📄 CANDIDATES
    // ─────────────────────────────────────────────────────────────
    @GET("api/v1/candidates")
    fun listCandidates(@Header("Authorization") token: String): Call<List<CandidateResponseDTO>>

    @Multipart
    @POST("api/v1/internal-applications/job-offer/{jobOfferId}")
    suspend fun createApplication(
        @Path("jobOfferId") jobOfferId: Long,
        @Part resume: MultipartBody.Part,
        @Header("Authorization") authHeader: String
    ): Response<Void>


    @POST("/api/v1/candidates")
    fun addCandidate(@Body request: CandidateRequestDTO): Call<CandidateResponseDTO>

    @PUT("/api/v1/candidates/{id}")
    fun updateCandidate(@Path("id") id: Long, @Body request: CandidateRequestDTO): Call<CandidateResponseDTO>

    @DELETE("/api/v1/candidates/{id}")
    fun deleteCandidate(@Path("id") id: Long): Call<Void>

    @GET("/api/v1/candidates/{id}")
    fun getCandidate(@Path("id") id: Long): Call<CandidateResponseDTO>


    // ─────────────────────────────────────────────────────────────
    // 🏥 MEDICAL VISITS & APPOINTMENTS
    // ─────────────────────────────────────────────────────────────
    @POST("api/v1/medical-visits")
    suspend fun submitMedicalVisit(
        @Header("Authorization") authHeader: String,
        @Body request: MedicalVisitRequest
    ): Response<Unit>

    @GET("api/v1/medical-visits")
    suspend fun getDoctorVisits(): Response<List<MedicalVisitResponse>>

    @GET("api/v1/medical-visits")
    suspend fun getMedicalVisits(@Header("Authorization") token: String): List<MedicalVisitResponse>

    @DELETE("api/v1/medical-visits/{id}")
    fun deleteMedicalVisit(
        @Path("id") visitId: Long,
        @Header("Authorization") token: String
    ): Call<MessageResponse>

    @POST("api/v1/appointments")
    suspend fun submitAppointment(
        @Header("Authorization") authToken: String,
        @Body request: AppointmentRequest
    ): Response<Unit>

    @GET("api/v1/appointments")
    suspend fun getAllAppointments(): Response<List<AppointmentResponseDTO>>

    @GET("api/v1/appointments/{id}")
    suspend fun getAppointmentById(@Path("id") id: Long): Response<AppointmentResponseDTO>

    @GET("api/v1/appointments/employee/{employeeId}")
    suspend fun getAppointmentsByEmployeeId(@Path("employeeId") employeeId: String): Response<List<AppointmentResponseDTO>>

    @GET("api/v1/appointments/medVisit/{medVisitId}")
    suspend fun getAppointmentsByMedVisitId(@Path("medVisitId") medVisitId: String): Response<List<AppointmentResponseDTO>>

    @POST("api/v1/appointments")
    suspend fun createAppointment(@Body appointmentRequest: AppointmentRequestDTO): Response<MessageResponse>

    @PUT("api/v1/appointments/{id}")
    suspend fun updateAppointment(@Path("id") id: Long, @Body appointmentRequest: AppointmentRequestDTO): Response<MessageResponse>

    @DELETE("api/v1/appointments/{id}")
    suspend fun deleteAppointment(@Path("id") id: Long): Response<MessageResponse>


    // ─────────────────────────────────────────────────────────────
    // 🔧 FACTORY METHOD
    // ─────────────────────────────────────────────────────────────
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun createWithToken(token: String): ApiService {
            return RetrofitClient.getClient(token).create(ApiService::class.java)
        }
    }
}
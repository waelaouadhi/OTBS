package com.example.onetechbs.network

import com.example.onetechbs.db.MessageResponseDTO
import com.example.onetechbs.db.TrainingRequest
import com.example.onetechbs.db.TrainingResponseDTO
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface TrainingService {
    // Remove the body - interface methods should not have implementation
    @GET("api/v1/trainings")
    suspend fun getTrainings(
        @Header("Authorization") token: String
    ): Response<List<TrainingResponseDTO>>

    @POST("api/v1/trainings")
    fun createTraining(
        @Header("Authorization") token: String,
        @Body trainingRequest: TrainingRequest
    ): Call<Void>

    @GET("api/v1/trainings/{id}")
    suspend fun getTrainingById(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): Response<TrainingResponseDTO>

    @PUT("api/v1/invitations/confirm/{id}")
    suspend fun confirmInvitation(
        @Header("Authorization") token: String,
        @Path("id") invitationId: Long
    ): Response<MessageResponseDTO>

    @GET("api/v1/trainings")
    fun getAllTrainings(): Call<List<TrainingResponseDTO>>
}
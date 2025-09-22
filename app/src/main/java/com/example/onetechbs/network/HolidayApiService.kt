package com.example.onetechbs.network

import com.example.onetechbs.model.Holiday
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface HolidayApiService {
    @GET("v1/holidays")
    suspend fun getHolidays(
        @Header("X-Api-Key") apiKey: String,
        @Query("country") country: String,
        @Query("year") year: Int,
        @Query("type") type: String = "public_holiday"
    ): Response<List<Holiday>>
}

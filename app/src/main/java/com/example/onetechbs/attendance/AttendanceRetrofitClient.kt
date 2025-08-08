package com.example.onetechbs.attendance

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AttendanceRetrofitClient {
    private const val BASE_URL = "http://172.31.4.45:8090/"

    val apiService: AttendanceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AttendanceApiService::class.java)
    }
}

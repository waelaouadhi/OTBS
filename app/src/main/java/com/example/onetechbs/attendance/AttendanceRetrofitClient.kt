package com.example.onetechbs.attendance

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AttendanceRetrofitClient {
    private const val BASE_URL = "http://192.168.1.184:8090/"

    val apiService: AttendanceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AttendanceApiService::class.java)
    }
}

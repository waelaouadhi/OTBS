package com.example.onetechbs.network

import android.util.Log
import com.example.onetechbs.db.NotificationResponse
import com.example.onetechbs.db.TrainingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val AUTH_BASE_URL = "http://192.168.0.190:8081/"
    private const val EMPLOYEE_BASE_URL = "http://192.168.0.190:8082/"
    private const val LEAVE_BASE_URL = "http://192.168.0.190:8083/"
    private const val TRAINING_BASE_URL = "http://192.168.0.190:8087/"
    private const val NOTIFICATION_BASE_URL = "http://192.168.0.190:8086/"
    private const val MED_BASE_URL = "http://192.168.0.190:8085/"

    private var token: String = "" // Store token in memory

    fun getAuthToken(): String = token

    fun setAuthToken(jwtToken: String) {
        token = jwtToken
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder: Request.Builder = chain.request().newBuilder()
            if (token.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
                Log.d("Request Headers", "Authorization: Bearer $token")
            } else {
                Log.d("Request Headers", "Token is missing!")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val leaveApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LEAVE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val authService: ApiService by lazy {
        createRetrofit(AUTH_BASE_URL).create(ApiService::class.java)
    }

    val employeeService: ApiService by lazy {
        createRetrofit(EMPLOYEE_BASE_URL).create(ApiService::class.java)
    }

    val leaveService: ApiService by lazy {
        createRetrofit(LEAVE_BASE_URL).create(ApiService::class.java)
    }

    val trainingService: ApiService by lazy {
        createRetrofit(TRAINING_BASE_URL).create(ApiService::class.java)
    }

    val notificationService: ApiService by lazy {
        createRetrofit(NOTIFICATION_BASE_URL).create(ApiService::class.java)
    }

    val medService: ApiService by lazy {
        createRetrofit(MED_BASE_URL).create(ApiService::class.java)
    }

    // Example suspend function for fetching notifications
//    suspend fun fetchNotifications(): List<NotificationResponse>? {
//        return withContext(Dispatchers.IO) {
//            try {
//                if (response.isSuccessful) {
//                    response.body()
//                } else {
//                    Log.e("RetrofitClient", "Failed to fetch notifications: ${response.code()}")
//                    null
//                }
//            } catch (e: Exception) {
//                Log.e("RetrofitClient", "Error fetching notifications", e)
//                null
//            }
//        }
//    }

    suspend fun fetchNotifications(): List<NotificationResponse>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = notificationService.getAllNotifications()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("RetrofitClient", "Failed to fetch notifications: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("RetrofitClient", "Error fetching notifications", e)
                null
            }
        }
    }
}
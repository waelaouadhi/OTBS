package com.example.onetechbs.network

import LocalDateAdapter
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.onetechbs.db.NotificationResponse
import com.example.onetechbs.util.LocalDateTimeAdapter
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
object RetrofitClient {

    private const val AUTH_BASE_URL = "http://172.31.4.154:8081/"
    private const val EMPLOYEE_BASE_URL = "http://172.31.4.154:8082/"
    private const val LEAVE_BASE_URL = "http://172.31.4.154:8083/"
    private const val TRAINING_BASE_URL = "http://172.31.4.154:8087/"
    private const val NOTIFICATION_BASE_URL = "http://172.31.4.154:8086/"
    private const val MED_BASE_URL = "http://172.31.4.154:8085/"
    private const val RECRUITING_BASE_URL = "http://172.31.4.154:8088/"
    private const val CONDIDATE_BASE_URL = "http://172.31.4.154:8089/"


    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    private var token: String = ""

    fun getAuthToken(): String = token

    fun setAuthToken(jwtToken: String) {
        token = jwtToken
        Log.d("RetrofitClient", "Auth token set: ${jwtToken.take(10)}...")
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        if (token.isNotBlank()) {
            if (chain.request().header("Authorization") == null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
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

    val trainingService2: ApiService by lazy {
        createRetrofit(TRAINING_BASE_URL).create(ApiService::class.java)
    }

    val trainingService: TrainingService by lazy {
        createRetrofit(TRAINING_BASE_URL).create(TrainingService::class.java)
    }

    val recruitingService: ApiService by lazy {
        createRetrofit(RECRUITING_BASE_URL).create(ApiService::class.java)
    }

    val candidateService: ApiService by lazy {
        createRetrofit(CONDIDATE_BASE_URL).create(ApiService::class.java)
    }

    val notificationService: ApiService by lazy {
        createRetrofit(NOTIFICATION_BASE_URL).create(ApiService::class.java)
    }

    val medService: ApiService by lazy {
        createRetrofit(MED_BASE_URL).create(ApiService::class.java)
    }

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


    fun getClient(token: String): Retrofit {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(RECRUITING_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(RECRUITING_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
    // ✅ ✅ NEW FUNCTION THAT READS FROM SHARED PREFS
    fun getInstance(context: Context): ApiService {
        val prefs = SharedPreferencesManager.getInstance(context)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = prefs.getAuthToken()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                    Log.d("RetrofitClient", "Token from SharedPreferences added")
                } else {
                    Log.w("RetrofitClient", "No token found in SharedPreferences")
                }
                chain.proceed(requestBuilder.build())
            }
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(RECRUITING_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    fun getTrainingService(context: Context): TrainingService {
        val prefs = SharedPreferencesManager.getInstance(context)
        val token = prefs.getAuthToken()

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                    Log.d("RetrofitClient", "✅ Token added to request: ${token.take(10)}...")
                } else {
                    Log.w("RetrofitClient", "⚠️ No auth token found")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()

        return Retrofit.Builder()
            .baseUrl(TRAINING_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TrainingService::class.java)
    }
}
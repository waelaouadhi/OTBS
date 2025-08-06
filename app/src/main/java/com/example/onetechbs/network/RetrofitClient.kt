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
    /** * Singleton object for Retrofit client to handle API requests.
     * * This object provides a single instance of Retrofit configured with
     * * base URLs for different services, interceptors for logging and authentication,
     *  * and methods to create service instances.
     *  *  It also includes methods to set and get authentication tokens,
     *  * and fetch notifications.
     *  *  * The base URLs are hardcoded for different services such as authentication,
     *  * employee management, leave management, training, notifications, medical services,
     *  * and recruiting.
     *  *  * The client is configured with timeouts for connection, read, and write operations,
     *  * and includes interceptors for adding authentication headers and logging requests and responses.
     *
     */
    private const val AUTH_BASE_URL = "http://172.31.4.218:8081/"
    private const val EMPLOYEE_BASE_URL = "http://172.31.4.218:8082/"
    private const val LEAVE_BASE_URL = "http://172.31.4.218:8083/"
    private const val TRAINING_BASE_URL = "http://172.31.4.218:8087/"
    private const val NOTIFICATION_BASE_URL = "http://172.31.4.218:8086/"
    private const val MED_BASE_URL = "http://172.31.4.218:8085/"
    private const val RECRUITING_BASE_URL = "http://172.31.4.218:8087/"
    private const val CONDIDATE_BASE_URL = "http://172.31.4.218:8089/"
    private const val DOCUMENTS_BASE_URL = "http://172.31.4.218:8093/"


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
            .registerTypeAdapter(java.time.Instant::class.java, com.example.onetechbs.util.InstantAdapter())
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

    val documentsService: ApiService by lazy {
        createRetrofit(DOCUMENTS_BASE_URL).create(ApiService::class.java)
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

    fun getRecruitingService(context: Context): ApiService {
        val prefs = SharedPreferencesManager.getInstance(context)
        val token = prefs.getAuthToken()
        val gson = com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime::class.java, com.example.onetechbs.util.LocalDateTimeAdapter())
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(RECRUITING_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    val candidateService: ApiService by lazy {
        createRetrofit(CONDIDATE_BASE_URL).create(ApiService::class.java)
    }

    val documentService: ApiService by lazy {
        createRetrofit(DOCUMENTS_BASE_URL).create(ApiService::class.java)
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
            .registerTypeAdapter(java.time.Instant::class.java, com.example.onetechbs.util.InstantAdapter())
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

        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(java.time.Instant::class.java, com.example.onetechbs.util.InstantAdapter())
            .create()

        return Retrofit.Builder()
            .baseUrl(TRAINING_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
    suspend fun proposeCourseProposition(context: Context, request: com.example.onetechbs.db.CoursePropositionRequestDTO): retrofit2.Response<Void> {
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
            .registerTypeAdapter(java.time.Instant::class.java, com.example.onetechbs.util.InstantAdapter())
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(TRAINING_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val api = retrofit.create(ApiService::class.java)
        return api.proposeCourse(request)
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
            .registerTypeAdapter(java.time.Instant::class.java, com.example.onetechbs.util.InstantAdapter())
            .create()

        return Retrofit.Builder()
            .baseUrl(TRAINING_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TrainingService::class.java)
    }
}
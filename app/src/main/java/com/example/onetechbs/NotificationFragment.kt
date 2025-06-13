package com.example.onetechbs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentNotificationBinding
import com.example.onetechbs.db.Notification
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val notificationAdapter = NotificationAdapter()
    private val TAG = "NotificationFragment"
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Fragment created")
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)

        sharedPreferencesManager = SharedPreferencesManager.getInstance(requireContext())

        setupRecyclerView()
        loadNotifications()

        binding.markAllReadButton.setOnClickListener {
            markAllAsRead()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun loadNotifications() {
        Log.d(TAG, "Loading notifications...")
        lifecycleScope.launch {
            val notifications = fetchNotifications()
            Log.d(TAG, "Fetched ${notifications.size} notifications")

            notificationAdapter.submitList(notifications)

            binding.emptyView.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
            Log.d(TAG, if (notifications.isEmpty()) "No notifications available" else "Displaying notifications")
        }
    }

    private suspend fun fetchNotifications(): List<Notification> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching notifications from API...")
        val client = OkHttpClient()
        var jwtToken = sharedPreferencesManager.getAuthToken()

        if (jwtToken.isNullOrEmpty()) {
            Log.e(TAG, "❌ No JWT token found, cannot fetch notifications.")
            return@withContext emptyList()
        }

        // ✅ Check if token expired & refresh if needed
        if (sharedPreferencesManager.isTokenExpired()) {
            Log.d(TAG, "🔧 JWT expired. Attempting refresh...")

            val refreshSuccess = refreshJwtToken()
            if (!refreshSuccess) {
                Log.e(TAG, "❌ JWT refresh failed. Staying logged in but cannot fetch notifications.")
                return@withContext emptyList()
            }

            jwtToken = sharedPreferencesManager.getAuthToken() ?: ""
        }

        val serverUrl = sharedPreferencesManager.getServerUrl() ?: "http://172.31.4.235:8086"
        val request = Request.Builder()
            .url("$serverUrl/api/v1/notifications")
            .header("Authorization", "Bearer $jwtToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "✅ API Response Code: ${response.code}")
                val responseBody = response.body?.string()
                Log.d(TAG, "✅ API Response Body: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonArray = JSONArray(responseBody)
                    return@withContext List(jsonArray.length()) { index ->
                        val obj = jsonArray.getJSONObject(index)
                        Notification(
                            id = obj.getLong("id"),
                            title = obj.getString("title"),
                            sender = obj.getString("sender"),
                            message = obj.getString("message"),
                            type = obj.getString("type"),
                            createdAt = obj.getString("createdAt"),
                            read = obj.getBoolean("read")
                        )
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch notifications: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching notifications", e)
        }

        return@withContext emptyList()
    }

    private suspend fun refreshJwtToken(): Boolean = withContext(Dispatchers.IO) {
        val refreshToken = sharedPreferencesManager.getRefreshToken()
        if (refreshToken == null) {
            Log.e(TAG, "❌ DEBUG: Refresh token is null")
            return@withContext false
        }

        val serverUrl = sharedPreferencesManager.getServerUrl() ?: "http://172.31.4.235:8086"
        val refreshUrl = "$serverUrl/api/v1/auth/refresh"

        Log.d(TAG, "🔍 DEBUG: Starting JWT refresh operation")
        Log.d(TAG, "🔍 DEBUG: Using server URL: $serverUrl")
        Log.d(TAG, "🔍 DEBUG: Refresh token length: ${refreshToken.length}, starts with: ${refreshToken.take(10)}...")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // Prepare JSON body with refresh token
        val jsonBody = JSONObject().put("refreshToken", refreshToken).toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(refreshUrl)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        try {
            Log.d(TAG, "🔍 DEBUG: Executing HTTP request...")
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                Log.d(TAG, "🔍 DEBUG: Response received")
                Log.d(TAG, "🔍 DEBUG: Response code: ${response.code}")
                Log.d(TAG, "🔍 DEBUG: Response message: ${response.message}")
                Log.d(TAG, "🔍 DEBUG: Response headers: ${response.headers}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ DEBUG: HTTP error: ${response.code} ${response.message}")
                    Log.e(TAG, "❌ DEBUG: Error response body: $responseBody")
                    return@withContext false
                }

                if (responseBody.isNullOrEmpty()) {
                    Log.e(TAG, "❌ DEBUG: Empty response body")
                    return@withContext false
                }

                try {
                    val json = JSONObject(responseBody)
                    val newAccessToken = json.optString("accessToken", "")
                    val newExpiration = json.optLong("accessExpiration", 0L)

                    if (newAccessToken.isEmpty() || newExpiration <= 0) {
                        Log.e(TAG, "❌ DEBUG: Invalid token or expiration in response")
                        return@withContext false
                    }

                    sharedPreferencesManager.saveAuthToken(newAccessToken, newExpiration)
                    Log.d(TAG, "✅ DEBUG: Token refreshed and saved successfully")
                    return@withContext true

                } catch (e: Exception) {
                    Log.e(TAG, "❌ DEBUG: JSON parsing error", e)
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ DEBUG: HTTP request failed", e)
            return@withContext false
        }
    }
    private fun markAllAsRead() {
        Log.d(TAG, "Marking all notifications as read (TODO: Implement API call)")
    }

    override fun onDestroyView() {
        Log.d(TAG, "Fragment destroyed")
        _binding = null
        super.onDestroyView()
    }
}
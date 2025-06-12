package com.example.onetechbs.websocket

import android.annotation.SuppressLint
import android.util.Log
import com.example.onetechbs.db.NotificationResponse
import com.google.gson.Gson
import okhttp3.*
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import java.util.concurrent.TimeUnit

object NotificationWebSocketManager {
    private val gson = Gson()
    private var stompClient: StompClient? = null
    private var isConnected = false
    private var currentAuthToken: String? = null

    /**
     * Creates a WebSocket with Authorization header using OkHttpClient.
     * Can be used if you want low-level websocket connection (not STOMP).
     */
    fun createWebSocketWithAuth(url: String, token: String, listener: WebSocketListener): WebSocket {
        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")  // Add JWT for handshake
            .build()

        return client.newWebSocket(request, listener)
    }

    /**
     * Connect to the STOMP WebSocket endpoint with the given JWT token.
     * It sets the Authorization header on connect.
     */
    @SuppressLint("CheckResult")
    fun connect(authToken: String? = null) {
        if (isConnected) {
            Log.w("WebSocketManager", "⚠️ WebSocket already connected.")
            return
        }

        if (authToken == null) {
            Log.e("WebSocketManager", "❌ Error: JWT Token is missing. WebSocket connection failed.")
            return
        }

        currentAuthToken = authToken

        // Create a custom OkHttpClient with the Authorization header
        val client = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Authorization", "Bearer $authToken")
                val request = builder.build()
                chain.proceed(request)
            }
            .build()

        // Initialize STOMP client with custom OkHttpClient
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://192.168.1.155/ws-notification/websocket", null, client)
            .withClientHeartbeat(30000)
            .withServerHeartbeat(30000)

        // Connect with headers (Auth header already added in the OkHttpClient)
        stompClient?.connect()

        // Listen for lifecycle events to track connection status
        stompClient?.lifecycle()?.subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.i("WebSocketManager", "✅ STOMP Connected")
                    isConnected = true
                    subscribeToNotifications()
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.i("WebSocketManager", "🚫 STOMP Disconnected")
                    isConnected = false
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("WebSocketManager", "❌ STOMP Error: ${event.exception?.message}")
                    isConnected = false
                }
                else -> {
                    Log.w("WebSocketManager", "⚠️ STOMP Event: ${event.type}")
                }
            }
        }
    }

    /**
     * Subscribe to the notifications topic to receive messages.
     */
    @SuppressLint("CheckResult")
    private fun subscribeToNotifications() {
        stompClient?.topic("/topic/notifications")?.subscribe { stompMessage: StompMessage ->
            Log.i("WebSocketManager", "📩 Received Notification: ${stompMessage.payload}")

            try {
                val notification = gson.fromJson(stompMessage.payload, NotificationResponse::class.java)
                handleNotification(notification)
            } catch (e: Exception) {
                Log.e("WebSocketManager", "❌ Error parsing notification: ${e.message}")
            }
        }
    }

    /**
     * Handle incoming notifications - currently logs the notification.
     */
    private fun handleNotification(notification: NotificationResponse) {
        Log.i("WebSocketManager", "📩 New Notification: $notification")
    }

    /**
     * Disconnect the WebSocket and cleanup.
     */
    fun disconnect() {
        stompClient?.disconnect()
        stompClient = null
        isConnected = false
        Log.i("WebSocketManager", "🚫 WebSocket Disconnected")
    }

    fun isConnected(): Boolean = isConnected
}
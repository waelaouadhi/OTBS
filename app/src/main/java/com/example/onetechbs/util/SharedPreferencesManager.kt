package com.example.onetechbs.util

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "OTBS_PREFS", Context.MODE_PRIVATE
    )

    companion object {
        @Volatile
        private var instance: SharedPreferencesManager? = null

        fun getInstance(context: Context): SharedPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesManager(context).also { instance = it }
            }
        }

        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_EXPIRATION = "access_expiration"
        private const val KEY_REFRESH_EXPIRATION = "refresh_expiration"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_DEPARTMENT = "user_department"
    }

    // 🔒 Token Methods
    fun saveAuthToken(token: String, expiration: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putLong(KEY_ACCESS_EXPIRATION, expiration)
            apply()
        }
    }

    fun getAuthToken(): String? = sharedPreferences.getString(KEY_AUTH_TOKEN, null)

    fun isTokenExpired(): Boolean {
        val expiration = sharedPreferences.getLong(KEY_ACCESS_EXPIRATION, 0)
        return System.currentTimeMillis() > expiration
    }

    fun saveRefreshToken(token: String, expiration: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_REFRESH_TOKEN, token)
            putLong(KEY_REFRESH_EXPIRATION, expiration)
            apply()
        }
    }

    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    fun isRefreshTokenExpired(): Boolean {
        val expiration = sharedPreferences.getLong(KEY_REFRESH_EXPIRATION, 0)
        return System.currentTimeMillis() > expiration
    }

    fun clearAuthData() {
        sharedPreferences.edit().clear().apply()
    }

    // 🔧 Server URL
    fun saveServerUrl(url: String) = sharedPreferences.edit().putString(KEY_SERVER_URL, url).apply()
    fun getServerUrl(): String? = sharedPreferences.getString(KEY_SERVER_URL, null)

    // 🔧 User Info
    fun saveUserId(userId: String) = sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)

    fun saveUserName(userName: String) = sharedPreferences.edit().putString(KEY_USER_NAME, userName).apply()
    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)

    fun saveUserEmail(userEmail: String) = sharedPreferences.edit().putString(KEY_USER_EMAIL, userEmail).apply()
    fun getUserEmail(): String? = sharedPreferences.getString(KEY_USER_EMAIL, null)

    fun saveUserRole(userRole: String) = sharedPreferences.edit().putString(KEY_USER_ROLE, userRole).apply()
    fun getUserRole(): String? = sharedPreferences.getString(KEY_USER_ROLE, null)

    fun saveUserDepartment(userDepartment: String) = sharedPreferences.edit().putString(KEY_USER_DEPARTMENT, userDepartment).apply()
    fun getUserDepartment(): String? = sharedPreferences.getString(KEY_USER_DEPARTMENT, null)
}
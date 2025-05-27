package com.example.onetechbs.network

import android.content.Context
import android.content.SharedPreferences

class UserSessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveJwtToken(token: String) {
        sharedPreferences.edit().putString("jwt_token", token).apply()
    }

    fun getJwtToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }

    fun clearJwtToken() {
        sharedPreferences.edit().remove("jwt_token").apply()
    }
}
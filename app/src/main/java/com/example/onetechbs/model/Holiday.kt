package com.example.onetechbs.model

import com.google.gson.annotations.SerializedName

data class Holiday(
    @SerializedName("name")
    val name: String,
    @SerializedName("date")
    val date: String, // Format: YYYY-MM-DD
    @SerializedName("type")
    val type: String,
    @SerializedName("country")
    val country: String
)

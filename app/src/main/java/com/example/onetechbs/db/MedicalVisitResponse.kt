package com.example.onetechbs.db

import com.google.gson.annotations.SerializedName

data class MedicalVisitResponse(
    val id: Long,
    @SerializedName("doctorName")
    val doctorName: String,
    @SerializedName("visitDate")
    val visitDate: String,
    @SerializedName("startTime")
    val startTime: String,
    @SerializedName("endTime")
    val endTime: String
)

package com.example.onetechbs.db

import java.math.BigDecimal
import com.google.gson.annotations.SerializedName

data class CourseResponseDTO(
    val id: Long,
    val title: String,
    val description: String,
    val hasCertification: Boolean,
    val cost: BigDecimal?,
    @SerializedName(value = "isEnrolled", alternate = ["enrolled", "IsEnrolled", "is_enrolled"]) val isEnrolled: Boolean,
    @SerializedName(value = "isRequested", alternate = ["requested", "IsRequested", "is_requested"]) val isRequested: Boolean
)

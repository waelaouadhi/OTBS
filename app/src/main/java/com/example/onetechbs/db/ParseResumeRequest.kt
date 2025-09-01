package com.example.onetechbs.db

import com.google.gson.annotations.SerializedName

data class ParseResumeRequest(
    val jobOfferId: Long,
    val candidateId: Long,
    val resume: String,
    @SerializedName("job_description") val jobDescription: String
)

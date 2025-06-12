package com.example.onetechbs.db

data class MatchResponse(
    val matchScore: Double,
    val details: String,
    val parsedResume: String,
    val jobInfo: JobDescription
) 
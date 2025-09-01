package com.example.onetechbs.network

import com.example.onetechbs.db.ParseResumeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApiService {
    @POST("/api/v1/match")
    suspend fun match(@Body request: ParseResumeRequest): Response<AiMatchResponse>
}

// Mapping for the backend response.
data class AiMatchResponse(
    val match_result: MatchResult?,
    val warnings: List<String>? = null
)

data class MatchResult(
    val score: Double?,
    val interpretation: String? = null,
    val red_flags: List<String>? = null,
    val warnings: List<String>? = null,
    // Optional flattened breakdowns (kept for forward-compat if backend adds them)
    val skills_score: Double? = null,
    val experience_score: Double? = null,
    val education_score: Double? = null,
    val certifications_score: Double? = null,
    // Optional skills matches (flattened)
    val matched_skills: List<String>? = null,
    val missing_skills: List<String>? = null,
    // Details section from backend (primary source for breakdowns)
    val details: Map<String, CriterionDetail>? = null
)

// Criterion detail structure inside match_result.details
// Keys are snake_case names like "skills_match", "relevant_experience", "education", "certifications", etc.
data class CriterionDetail(
    val raw_score: Double? = null,          // 0..10
    val weighted_score: Double? = null,     // in weight points (0..weight)
    val matching_skills: List<String>? = null,
    val missing_skills: List<String>? = null,
    val analysis: String? = null
)

package com.example.onetechbs.db

import com.google.gson.annotations.SerializedName

data class JobDetailsRequest(
    @SerializedName("job_title")
    val jobTitle: String,
    @SerializedName("department")
    val department: String,
    @SerializedName("experience_level")
    val experienceLevel: String,
    @SerializedName("key_responsibilities")
    val keyResponsibilities: List<String>,
    @SerializedName("must_have_skills")
    val mustHaveSkills: List<String>,
    @SerializedName("nice_to_have_skills")
    val niceToHaveSkills: List<String>,
    @SerializedName("team_vibe")
    val teamVibe: String
)

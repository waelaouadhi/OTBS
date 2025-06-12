package com.example.onetechbs.network

import com.example.onetechbs.db.JobDescription
import com.example.onetechbs.db.MatchResponse
import com.example.onetechbs.db.RoleEvaluationResponse
import okhttp3.MultipartBody
import retrofit2.Response

class ResumeService(private val apiService: ApiService) {

//    suspend fun matchResumeWithJob(jobDescription: JobDescription): Response<MatchResponse> {
//        return apiService.matchResumeWithJob(jobDescription)
//    }
//
//    suspend fun parseResume(resume: MultipartBody.Part): Response<String> {
//        return apiService.parseResume(resume)
//    }
//
//    suspend fun evaluateRole(jobDescription: JobDescription): Response<RoleEvaluationResponse> {
//        return apiService.evaluateRole(jobDescription)
//    }
}
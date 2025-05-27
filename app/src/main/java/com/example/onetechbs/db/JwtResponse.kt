package com.example.onetechbs.db

data class JwtResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiration: Long,
    val refreshExpiration: Long,
    val employee: EmployeeResponse  // This holds the employee data
)


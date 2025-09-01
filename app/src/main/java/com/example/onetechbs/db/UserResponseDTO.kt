package com.example.onetechbs.db

data class UserResponseDTO(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val jobTitle: String,
    val department: String,
    val phoneNumber1: String,
    val phoneNumber2: String?,
    val gender: String?,
    val birthDate: String?,
    val role: String,
    // Backend returns Base64 string here (not JSON array of numbers), so keep as String?
    val picture: String?,
    val pictureType: String?
)
package com.example.onetechbs.db

data class ApplicationResponseDTO(
    val id: Long,
    val candidateId: Long,
    val fullName: String,
    val isInternal: Boolean,
    val email: String,
    val phone: String,
    val status: String,  // Map EApplicationStatus as String
    val score: Double,
    val submissionDate: String // LocalDateTime → ISO String (adjust format if needed)
)
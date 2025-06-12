package com.example.onetechbs.db
data class LanguageDTO(
    val id: Long,
    val language: String,
    val proficiency: EProficiency
)
enum class EProficiency {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    FLUENT
}
package com.sloth.registerapp.features.auth.domain.model

enum class LoginAttemptStatus {
    SUCCESS,
    FAILED
}

data class LoginHistory(
    val id: String,
    val ownerUserId: String,
    val usernameSnapshot: String,
    val deviceLabel: String?,
    val ipOrNetwork: String?,
    val status: LoginAttemptStatus,
    val createdAtMs: Long
)

package com.sloth.registerapp.features.auth.data.remote.dto

data class RegisterRequestDto(
    val username: String,
    val email: String,
    val password: String
)

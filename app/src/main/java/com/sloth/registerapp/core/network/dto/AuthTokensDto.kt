package com.sloth.registerapp.core.network.dto

import com.google.gson.annotations.SerializedName

data class AuthTokensDto(
    val token: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val userId: String? = null,
    @SerializedName("user_id") val userIdSnake: String? = null,
    val username: String? = null,
    val email: String? = null
) {
    fun resolvedAccessToken(): String? = accessToken ?: token

    fun resolvedUserId(): String = userId ?: userIdSnake ?: ""
}

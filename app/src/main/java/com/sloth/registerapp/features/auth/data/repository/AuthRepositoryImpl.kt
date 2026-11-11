package com.sloth.registerapp.features.auth.data.repository

import android.util.Base64
import com.sloth.registerapp.core.network.dto.AuthTokensDto
import com.sloth.registerapp.core.network.SdiaApiService

class AuthRepositoryImpl(private val apiService: SdiaApiService) {
    suspend fun loginUser(username: String, password: String): Result<AuthTokensDto> {
        return try {
            val credentials = "$username:$password"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val response = apiService.login(basicAuth)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

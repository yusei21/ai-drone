package com.sloth.registerapp.core.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.SessionManager
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.auth.model.ServerAuthState
import com.sloth.registerapp.core.network.dto.AuthTokensDto
import com.sloth.registerapp.core.observability.AppObservability
import io.sentry.SentryLevel
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import kotlinx.coroutines.runBlocking

class TokenRefreshAuthenticator(
    context: Context,
    private val tokenRepository: TokenRepository
) : Authenticator {
    companion object {
        private const val TAG = "TokenRefreshAuth"
    }

    private val appContext = context.applicationContext
    private val refreshClient = OkHttpClient()
    private val gson = Gson()
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (!AuthRoutePolicy.requiresAccessToken(path) || AuthRoutePolicy.isAuthRoute(path)) {
            return null
        }
        if (responseCount(response) >= 2) {
            AppObservability.breadcrumb(
                category = "auth",
                message = "Token refresh skipped after repeated auth failures",
                level = SentryLevel.WARNING,
                data = mapOf("path" to path)
            )
            return null
        }

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer")
            ?.trim()

        synchronized(lock) {
            val latestAccessToken = tokenRepository.getAccessTokenBlocking()
            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .build()
            }

            val refreshToken = tokenRepository.getRefreshTokenBlocking() ?: return clearSessionAndAbort()
            AppObservability.breadcrumb(
                category = "auth",
                message = "Token refresh started",
                data = mapOf("path" to path)
            )
            val refreshed = refreshTokens(refreshToken) ?: return clearSessionAndAbort()
            val newAccessToken = refreshed.resolvedAccessToken() ?: return clearSessionAndAbort()
            val newRefreshToken = refreshed.refreshToken ?: refreshToken

            tokenRepository.saveTokensBlocking(newAccessToken, newRefreshToken)
            runBlocking {
                LocalSessionManager.getInstance(appContext)
                    .setServerAuthState(ServerAuthState.SERVER_AUTH_OK)
            }
            Log.i(TAG, "Token refresh succeeded")
            AppObservability.breadcrumb(
                category = "auth",
                message = "Token refresh succeeded"
            )
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }
    }

    private fun refreshTokens(refreshToken: String): AuthTokensDto? {
        val refreshUrl = "${RetrofitClient.baseUrl()}auth/refresh"
        val headerRequest = Request.Builder()
            .url(refreshUrl)
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Authorization", "Bearer $refreshToken")
            .build()

        try {
            refreshClient.newCall(headerRequest).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body?.charStream()?.use { reader ->
                        gson.fromJson(reader, AuthTokensDto::class.java)
                    }
                }
            }
        } catch (e: IOException) {
            AppObservability.breadcrumb(
                category = "auth",
                message = "Header-style token refresh failed, trying body fallback",
                level = SentryLevel.WARNING,
                data = mapOf("error_type" to e::class.java.simpleName)
            )
            // Fallback below (body style).
        }

        val bodyJson = gson.toJson(mapOf("refresh_token" to refreshToken))
        val bodyRequest = Request.Builder()
            .url(refreshUrl)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            refreshClient.newCall(bodyRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.charStream()?.use { reader ->
                    gson.fromJson(reader, AuthTokensDto::class.java)
                }
            }
        } catch (e: IOException) {
            AppObservability.captureException(
                throwable = e,
                area = "auth",
                operation = "refresh_token",
                level = SentryLevel.WARNING,
                tags = mapOf("exception.kind" to "io")
            )
            null
        }
    }

    private fun clearSessionAndAbort(): Request? {
        Log.w(TAG, "Token refresh failed; marking server auth required and clearing tokens")
        AppObservability.captureMessage(
            message = "Token refresh failed; local session was cleared",
            area = "auth",
            operation = "refresh_token",
            level = SentryLevel.WARNING
        )
        tokenRepository.clearTokensBlocking()
        runBlocking {
            SessionManager.getInstance(appContext).clearSession()
            LocalSessionManager.getInstance(appContext)
                .setServerAuthState(ServerAuthState.SERVER_AUTH_REQUIRED)
        }
        return null
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var current = response.priorResponse
        while (current != null) {
            result++
            current = current.priorResponse
        }
        return result
    }
}

package com.sloth.registerapp.core.network

import com.sloth.registerapp.core.auth.TokenRepository
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenRepository: TokenRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (!AuthRoutePolicy.requiresAccessToken(path)) {
            return chain.proceed(request)
        }

        // Se a requisição já possui Authorization, não duplica o header
        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }

        val token = tokenRepository.getAccessTokenBlocking()
        val requestBuilder = request.newBuilder()
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        return chain.proceed(requestBuilder.build())
    }
}

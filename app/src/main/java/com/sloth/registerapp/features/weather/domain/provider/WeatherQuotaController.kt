package com.sloth.registerapp.features.weather.domain.provider

interface WeatherQuotaController {
    suspend fun canCall(providerId: String): Boolean
    suspend fun recordCall(providerId: String)
    suspend fun recordRateLimit(providerId: String)
}

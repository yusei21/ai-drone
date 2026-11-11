package com.sloth.registerapp.features.weather.domain.provider

import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot

interface WeatherProviderRouter {
    suspend fun getCurrentWithFallback(lat: Double, lon: Double): WeatherSnapshot
    suspend fun getHourlyWithFallback(lat: Double, lon: Double): List<WeatherSnapshot>
}

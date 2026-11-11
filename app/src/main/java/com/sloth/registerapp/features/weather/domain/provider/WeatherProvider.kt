package com.sloth.registerapp.features.weather.domain.provider

import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot

interface WeatherProvider {
    val providerId: String

    suspend fun getCurrent(lat: Double, lon: Double): WeatherSnapshot
    suspend fun getHourly(lat: Double, lon: Double): List<WeatherSnapshot>
}

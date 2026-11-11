package com.sloth.registerapp.features.weather.data.provider

import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.sloth.registerapp.features.weather.data.remote.WeatherRetrofitFactory
import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot
import com.sloth.registerapp.features.weather.domain.provider.WeatherProvider

class WeatherApiProvider : WeatherProvider {
    override val providerId: String = WeatherProviderSettingsRepository.PROVIDER_WEATHER_API

    override suspend fun getCurrent(lat: Double, lon: Double): WeatherSnapshot {
        val apiKey = BuildConfig.WEATHER_API_KEY
        require(apiKey.isNotBlank()) { "WEATHER_API_KEY não configurada" }

        val response = WeatherRetrofitFactory.weatherApi().getCurrent(
            apiKey = apiKey,
            query = "$lat,$lon"
        )
        val current = response.current ?: throw IllegalStateException("WeatherAPI sem dados atuais")
        val conditionCode = current.condition?.code?.toString()
        val lightningRisk = if (conditionCode in THUNDER_CODES) 0.9 else 0.0

        return WeatherSnapshot(
            timestampMs = System.currentTimeMillis(),
            temperatureC = current.tempC,
            rainMm = current.precipMm,
            precipitationProbabilityPercent = null,
            windSpeedMs = current.windKph?.div(3.6),
            windGustMs = current.gustKph?.div(3.6),
            lightningRisk = lightningRisk,
            conditionCode = conditionCode,
            providerId = providerId
        )
    }

    override suspend fun getHourly(lat: Double, lon: Double): List<WeatherSnapshot> {
        val apiKey = BuildConfig.WEATHER_API_KEY
        require(apiKey.isNotBlank()) { "WEATHER_API_KEY não configurada" }

        val response = WeatherRetrofitFactory.weatherApi().getForecast(
            apiKey = apiKey,
            query = "$lat,$lon"
        )
        val hourly = response.forecast
            ?.forecastDays
            ?.firstOrNull()
            ?.hourly
            .orEmpty()

        if (hourly.isEmpty()) return listOf(getCurrent(lat, lon))

        return hourly.take(HOURLY_LIMIT).map { hour ->
            val conditionCode = hour.condition?.code?.toString()
            WeatherSnapshot(
                timestampMs = (hour.timeEpochSeconds ?: (System.currentTimeMillis() / 1000L)) * 1000L,
                temperatureC = hour.tempC,
                rainMm = hour.precipMm,
                precipitationProbabilityPercent = hour.chanceOfRainPercent?.toDouble(),
                windSpeedMs = hour.windKph?.div(3.6),
                windGustMs = hour.gustKph?.div(3.6),
                lightningRisk = if (conditionCode in THUNDER_CODES) 0.9 else 0.0,
                conditionCode = conditionCode,
                providerId = providerId
            )
        }
    }

    companion object {
        private val THUNDER_CODES = setOf("1087", "1273", "1276", "1279", "1282")
        private const val HOURLY_LIMIT = 12
    }
}

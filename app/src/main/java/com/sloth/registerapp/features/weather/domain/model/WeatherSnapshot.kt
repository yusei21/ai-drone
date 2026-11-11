package com.sloth.registerapp.features.weather.domain.model

data class WindProfileLevel(
    val heightMeters: Int,
    val speedMs: Double?
)

data class WeatherSnapshot(
    val timestampMs: Long,
    val temperatureC: Double?,
    val rainMm: Double?,
    val precipitationProbabilityPercent: Double?,
    val windSpeedMs: Double?,
    val windGustMs: Double?,
    val windProfiles: List<WindProfileLevel> = emptyList(),
    val lightningRisk: Double?,
    val conditionCode: String?,
    val providerId: String
)

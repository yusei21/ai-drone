package com.sloth.registerapp.features.weather.domain.model

data class WeatherSummary(
    val snapshot: WeatherSnapshot,
    val decision: WeatherSafetyDecision
)

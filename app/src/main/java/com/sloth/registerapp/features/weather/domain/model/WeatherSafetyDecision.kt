package com.sloth.registerapp.features.weather.domain.model

data class WeatherSafetyDecision(
    val level: WeatherSafetyLevel,
    val shortMessage: String,
    val reasons: List<String>
)

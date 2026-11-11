package com.sloth.registerapp.features.weather.domain.usecase

import com.sloth.registerapp.features.weather.data.repository.WeatherRepositoryImpl
import com.sloth.registerapp.features.weather.domain.model.WeatherSummary

class GetWeatherSummaryUseCase(
    private val repository: WeatherRepositoryImpl,
    private val decisionUseCase: BuildWeatherSafetyDecisionUseCase = BuildWeatherSafetyDecisionUseCase()
) {
    suspend operator fun invoke(lat: Double, lon: Double, ttlMs: Long = 60_000L): WeatherSummary {
        val snapshot = repository.getCurrent(lat, lon, ttlMs)
        val decision = decisionUseCase(snapshot)
        return WeatherSummary(snapshot, decision)
    }
}

package com.sloth.registerapp.features.weather.domain.usecase

import com.sloth.registerapp.features.weather.data.repository.WeatherRepositoryImpl
import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot

class GetWeatherDetailUseCase(
    private val repository: WeatherRepositoryImpl
) {
    suspend operator fun invoke(lat: Double, lon: Double): List<WeatherSnapshot> {
        return repository.getHourly(lat, lon)
    }
}

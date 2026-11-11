package com.sloth.registerapp.features.weather.data.repository

import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot
import com.sloth.registerapp.features.weather.domain.provider.WeatherProviderRouter

class WeatherRepositoryImpl(
    private val router: WeatherProviderRouter,
    private val cache: WeatherCacheRepository = WeatherCacheRepository()
) {
    suspend fun getCurrent(lat: Double, lon: Double, ttlMs: Long = 60_000L): WeatherSnapshot {
        cache.getCurrent(lat, lon, ttlMs)?.let { return it }
        val fresh = router.getCurrentWithFallback(lat, lon)
        cache.putCurrent(lat, lon, fresh)
        return fresh
    }

    suspend fun getHourly(lat: Double, lon: Double): List<WeatherSnapshot> {
        return router.getHourlyWithFallback(lat, lon)
    }
}

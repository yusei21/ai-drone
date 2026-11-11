package com.sloth.registerapp.features.weather.data.repository

import com.sloth.registerapp.features.weather.domain.model.WeatherSnapshot

class WeatherCacheRepository {
    private val cache = mutableMapOf<String, Pair<Long, WeatherSnapshot>>()

    fun getCurrent(lat: Double, lon: Double, ttlMs: Long): WeatherSnapshot? {
        val key = key(lat, lon)
        val entry = cache[key] ?: return null
        return if (System.currentTimeMillis() - entry.first <= ttlMs) entry.second else null
    }

    fun putCurrent(lat: Double, lon: Double, snapshot: WeatherSnapshot) {
        cache[key(lat, lon)] = System.currentTimeMillis() to snapshot
    }

    private fun key(lat: Double, lon: Double): String {
        val latRounded = "%.3f".format(lat)
        val lonRounded = "%.3f".format(lon)
        return "$latRounded,$lonRounded"
    }
}

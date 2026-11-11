package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.weatherQuotaUsageDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_quota_usage")

data class WeatherProviderQuotaUsage(
    val minuteWindowStartMs: Long,
    val minuteCount: Int,
    val hourWindowStartMs: Long,
    val hourCount: Int,
    val dayWindowStartMs: Long,
    val dayCount: Int,
    val blockedUntilMs: Long
) {
    companion object {
        fun default(nowMs: Long): WeatherProviderQuotaUsage {
            return WeatherProviderQuotaUsage(
                minuteWindowStartMs = nowMs,
                minuteCount = 0,
                hourWindowStartMs = nowMs,
                hourCount = 0,
                dayWindowStartMs = nowMs,
                dayCount = 0,
                blockedUntilMs = 0L
            )
        }
    }
}

class WeatherQuotaUsageRepository private constructor(private val context: Context) {

    suspend fun getUsage(providerId: String): WeatherProviderQuotaUsage {
        val now = System.currentTimeMillis()
        val safeId = providerId.toSafePreferenceKey()
        val prefs = context.weatherQuotaUsageDataStore.data.first()
        return WeatherProviderQuotaUsage(
            minuteWindowStartMs = prefs[longPreferencesKey("${safeId}_minute_window_start_ms")] ?: now,
            minuteCount = prefs[intPreferencesKey("${safeId}_minute_count")] ?: 0,
            hourWindowStartMs = prefs[longPreferencesKey("${safeId}_hour_window_start_ms")] ?: now,
            hourCount = prefs[intPreferencesKey("${safeId}_hour_count")] ?: 0,
            dayWindowStartMs = prefs[longPreferencesKey("${safeId}_day_window_start_ms")] ?: now,
            dayCount = prefs[intPreferencesKey("${safeId}_day_count")] ?: 0,
            blockedUntilMs = prefs[longPreferencesKey("${safeId}_blocked_until_ms")] ?: 0L
        )
    }

    suspend fun saveUsage(providerId: String, usage: WeatherProviderQuotaUsage) {
        val safeId = providerId.toSafePreferenceKey()
        context.weatherQuotaUsageDataStore.edit { prefs ->
            prefs[longPreferencesKey("${safeId}_minute_window_start_ms")] = usage.minuteWindowStartMs
            prefs[intPreferencesKey("${safeId}_minute_count")] = usage.minuteCount
            prefs[longPreferencesKey("${safeId}_hour_window_start_ms")] = usage.hourWindowStartMs
            prefs[intPreferencesKey("${safeId}_hour_count")] = usage.hourCount
            prefs[longPreferencesKey("${safeId}_day_window_start_ms")] = usage.dayWindowStartMs
            prefs[intPreferencesKey("${safeId}_day_count")] = usage.dayCount
            prefs[longPreferencesKey("${safeId}_blocked_until_ms")] = usage.blockedUntilMs
        }
    }

    private fun String.toSafePreferenceKey(): String {
        return lowercase().replace(Regex("[^a-z0-9_]"), "_")
    }

    companion object {
        @Volatile
        private var INSTANCE: WeatherQuotaUsageRepository? = null

        fun getInstance(context: Context): WeatherQuotaUsageRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WeatherQuotaUsageRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

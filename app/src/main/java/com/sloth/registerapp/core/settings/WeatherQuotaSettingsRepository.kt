package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherQuotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_quota_settings")

data class WeatherQuotaPolicy(
    val maxCallsPerMinute: Int,
    val maxCallsPerHour: Int,
    val maxCallsPerDay: Int,
    val cooldownMinutesOn429: Int,
    val prototypeMode: Boolean
)

class WeatherQuotaSettingsRepository private constructor(private val context: Context) {

    private val callsPerMinuteKey = intPreferencesKey("weather_calls_per_minute")
    private val callsPerHourKey = intPreferencesKey("weather_calls_per_hour")
    private val callsPerDayKey = intPreferencesKey("weather_calls_per_day")
    private val cooldownMinutesKey = intPreferencesKey("weather_cooldown_minutes")
    private val prototypeModeKey = booleanPreferencesKey("weather_prototype_mode")

    val policy: Flow<WeatherQuotaPolicy> = context.weatherQuotaDataStore.data.map { prefs ->
        WeatherQuotaPolicy(
            maxCallsPerMinute = prefs[callsPerMinuteKey] ?: DEFAULT_CALLS_PER_MINUTE,
            maxCallsPerHour = prefs[callsPerHourKey] ?: DEFAULT_CALLS_PER_HOUR,
            maxCallsPerDay = prefs[callsPerDayKey] ?: DEFAULT_CALLS_PER_DAY,
            cooldownMinutesOn429 = prefs[cooldownMinutesKey] ?: DEFAULT_COOLDOWN_MINUTES,
            prototypeMode = prefs[prototypeModeKey] ?: true
        )
    }

    suspend fun setPolicy(policy: WeatherQuotaPolicy) {
        context.weatherQuotaDataStore.edit { prefs ->
            prefs[callsPerMinuteKey] = policy.maxCallsPerMinute
            prefs[callsPerHourKey] = policy.maxCallsPerHour
            prefs[callsPerDayKey] = policy.maxCallsPerDay
            prefs[cooldownMinutesKey] = policy.cooldownMinutesOn429
            prefs[prototypeModeKey] = policy.prototypeMode
        }
    }

    companion object {
        private const val DEFAULT_CALLS_PER_MINUTE = 10
        private const val DEFAULT_CALLS_PER_HOUR = 240
        private const val DEFAULT_CALLS_PER_DAY = 3000
        private const val DEFAULT_COOLDOWN_MINUTES = 15

        @Volatile
        private var INSTANCE: WeatherQuotaSettingsRepository? = null

        fun getInstance(context: Context): WeatherQuotaSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WeatherQuotaSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

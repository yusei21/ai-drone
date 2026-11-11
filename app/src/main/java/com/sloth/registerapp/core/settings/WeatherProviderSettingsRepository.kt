package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherProviderDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_provider_settings")

class WeatherProviderSettingsRepository private constructor(private val context: Context) {

    private val primaryProviderKey = stringPreferencesKey("weather_primary_provider")
    private val fallbackProviderKey = stringPreferencesKey("weather_fallback_provider")

    val primaryProvider: Flow<String> = context.weatherProviderDataStore.data.map { prefs ->
        prefs[primaryProviderKey] ?: PROVIDER_WEATHER_API
    }

    val fallbackProvider: Flow<String> = context.weatherProviderDataStore.data.map { prefs ->
        prefs[fallbackProviderKey] ?: PROVIDER_OPEN_METEO
    }

    suspend fun setPrimaryProvider(provider: String) {
        context.weatherProviderDataStore.edit { prefs ->
            prefs[primaryProviderKey] = provider
        }
    }

    suspend fun setFallbackProvider(provider: String) {
        context.weatherProviderDataStore.edit { prefs ->
            prefs[fallbackProviderKey] = provider
        }
    }

    companion object {
        const val PROVIDER_WEATHER_API = "WEATHER_API"
        const val PROVIDER_OPEN_METEO = "OPEN_METEO"

        @Volatile
        private var INSTANCE: WeatherProviderSettingsRepository? = null

        fun getInstance(context: Context): WeatherProviderSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = WeatherProviderSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

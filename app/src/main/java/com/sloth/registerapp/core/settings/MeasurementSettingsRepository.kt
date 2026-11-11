package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.measurementDataStore: DataStore<Preferences> by preferencesDataStore(name = "measurement_settings")

class MeasurementSettingsRepository private constructor(private val context: Context) {

    private val measurementSystemKey = stringPreferencesKey("measurement_system")

    val measurementSystem: Flow<String> = context.measurementDataStore.data.map { prefs ->
        prefs[measurementSystemKey] ?: SYSTEM_METRIC
    }

    suspend fun setMeasurementSystem(system: String) {
        context.measurementDataStore.edit { prefs ->
            prefs[measurementSystemKey] = system
        }
    }

    companion object {
        const val SYSTEM_METRIC = "METRIC"
        const val SYSTEM_IMPERIAL = "IMPERIAL"

        @Volatile
        private var INSTANCE: MeasurementSettingsRepository? = null

        fun getInstance(context: Context): MeasurementSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MeasurementSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

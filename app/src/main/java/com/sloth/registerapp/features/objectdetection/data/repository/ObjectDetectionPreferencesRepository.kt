package com.sloth.registerapp.features.objectdetection.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.objectDetectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "object_detection_settings"
)

class ObjectDetectionPreferencesRepository private constructor(
    private val context: Context
) {

    private val activeModelKey = stringPreferencesKey("object_detection_active_model_id")
    private val detectionEnabledKey = booleanPreferencesKey("object_detection_enabled")

    val activeModelId: Flow<String> = context.objectDetectionDataStore.data.map { prefs ->
        prefs[activeModelKey] ?: DEFAULT_MODEL_ID
    }

    val detectionEnabled: Flow<Boolean> = context.objectDetectionDataStore.data.map { prefs ->
        prefs[detectionEnabledKey] ?: false
    }

    suspend fun setActiveModelId(modelId: String) {
        context.objectDetectionDataStore.edit { prefs ->
            prefs[activeModelKey] = modelId
        }
    }

    suspend fun setDetectionEnabled(enabled: Boolean) {
        context.objectDetectionDataStore.edit { prefs ->
            prefs[detectionEnabledKey] = enabled
        }
    }

    companion object {
        const val DEFAULT_MODEL_ID = "person-detector"

        @Volatile
        private var INSTANCE: ObjectDetectionPreferencesRepository? = null

        fun getInstance(context: Context): ObjectDetectionPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ObjectDetectionPreferencesRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

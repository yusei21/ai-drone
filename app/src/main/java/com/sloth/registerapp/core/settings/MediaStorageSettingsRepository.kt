package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mediaStorageDataStore: DataStore<Preferences> by preferencesDataStore(name = "media_storage_settings")

class MediaStorageSettingsRepository private constructor(private val context: Context) {

    private val mediaTargetKey = stringPreferencesKey("media_storage_target")

    val mediaStorageTarget: Flow<String> = context.mediaStorageDataStore.data.map { prefs ->
        prefs[mediaTargetKey] ?: TARGET_PHONE
    }

    suspend fun setMediaStorageTarget(target: String) {
        context.mediaStorageDataStore.edit { prefs ->
            prefs[mediaTargetKey] = target
        }
    }

    companion object {
        const val TARGET_PHONE = "PHONE"
        const val TARGET_DRONE_SD = "DRONE_SD"

        @Volatile
        private var INSTANCE: MediaStorageSettingsRepository? = null

        fun getInstance(context: Context): MediaStorageSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MediaStorageSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

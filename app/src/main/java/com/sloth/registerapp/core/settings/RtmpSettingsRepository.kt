package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rtmpDataStore: DataStore<Preferences> by preferencesDataStore(name = "rtmp_settings")

class RtmpSettingsRepository private constructor(private val context: Context) {

    private val rtmpUrlKey = stringPreferencesKey("rtmp_url")

    val rtmpUrl: Flow<String> = context.rtmpDataStore.data.map { prefs ->
        prefs[rtmpUrlKey] ?: DEFAULT_URL
    }

    suspend fun setRtmpUrl(url: String) {
        context.rtmpDataStore.edit { prefs ->
            prefs[rtmpUrlKey] = url
        }
    }

    companion object {
        const val DEFAULT_URL = "rtmp://192.168.2.45:1935/live/drone_mission_1"

        @Volatile
        private var INSTANCE: RtmpSettingsRepository? = null

        fun getInstance(context: Context): RtmpSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = RtmpSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

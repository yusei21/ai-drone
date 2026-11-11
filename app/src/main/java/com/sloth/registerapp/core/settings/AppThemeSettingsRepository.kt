package com.sloth.registerapp.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class AppThemeSettingsRepository private constructor(private val context: Context) {

    private val appThemeKey = stringPreferencesKey("app_theme")

    val appTheme: Flow<String> = context.themeDataStore.data.map { prefs ->
        prefs[appThemeKey] ?: DEFAULT_THEME
    }

    suspend fun setAppTheme(theme: String) {
        context.themeDataStore.edit { prefs ->
            prefs[appThemeKey] = theme
        }
    }

    companion object {
        const val THEME_LIGHT = "Claro"
        const val THEME_DARK = "Escuro"
        const val THEME_SYSTEM = "Padrão do Sistema"
        const val DEFAULT_THEME = THEME_SYSTEM

        @Volatile
        private var INSTANCE: AppThemeSettingsRepository? = null

        fun getInstance(context: Context): AppThemeSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppThemeSettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

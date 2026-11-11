package com.sloth.registerapp.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager private constructor(private val context: Context) {

    private val tokenKey = stringPreferencesKey("session_token")
    private val userIdKey = stringPreferencesKey("session_user_id")
    private val usernameKey = stringPreferencesKey("session_username")
    private val emailKey = stringPreferencesKey("session_email")
    private val sessionCreatedAtKey = longPreferencesKey("session_created_at")
    private val lastSyncAtKey = longPreferencesKey("last_sync_at")
    private val sessionExpiryDaysKey = longPreferencesKey("session_expiry_days")

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[tokenKey]
    }

    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[userIdKey]
    }

    val username: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[usernameKey]
    }

    val email: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[emailKey]
    }

    val lastSyncAt: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[lastSyncAtKey] ?: 0L
    }

    val isSessionActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val token = preferences[tokenKey]
        val createdAt = preferences[sessionCreatedAtKey]
        val expiryDays = preferences[sessionExpiryDaysKey]

        if (token != null && createdAt != null && expiryDays != null) {
            val expiryMillis = TimeUnit.DAYS.toMillis(expiryDays)
            (System.currentTimeMillis() - createdAt) < expiryMillis
        } else {
            false
        }
    }

    suspend fun createSession(
        token: String,
        userId: String,
        username: String,
        email: String,
        expiryDays: Long = 30L
    ) {
        context.dataStore.edit { preferences ->
            preferences[tokenKey] = token
            preferences[userIdKey] = userId
            preferences[usernameKey] = username
            preferences[emailKey] = email
            preferences[sessionCreatedAtKey] = System.currentTimeMillis()
            preferences[sessionExpiryDaysKey] = expiryDays
        }
    }

    suspend fun extendSession(additionalDays: Long = 7L) {
        context.dataStore.edit { preferences ->
            val currentExpiry = preferences[sessionExpiryDaysKey] ?: 30L
            preferences[sessionExpiryDaysKey] = currentExpiry + additionalDays
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(userIdKey)
            preferences.remove(usernameKey)
            preferences.remove(emailKey)
            preferences.remove(sessionCreatedAtKey)
            preferences.remove(sessionExpiryDaysKey)
        }
    }

    suspend fun updateLastSyncTime() {
        context.dataStore.edit { preferences ->
            preferences[lastSyncAtKey] = System.currentTimeMillis()
        }
    }

    suspend fun getTokenBlocking(): String? {
        val preferences = context.dataStore.data.first()
        val token = preferences[tokenKey]
        val createdAt = preferences[sessionCreatedAtKey]
        val expiryDays = preferences[sessionExpiryDaysKey]

        return if (token != null && createdAt != null && expiryDays != null) {
            val expiryMillis = TimeUnit.DAYS.toMillis(expiryDays)
            if ((System.currentTimeMillis() - createdAt) < expiryMillis) {
                token
            } else {
                clearSession()
                null
            }
        } else {
            null
        }
    }

    suspend fun getSessionDataBlocking(): Triple<String?, String?, String?> {
        val preferences = context.dataStore.data.first()
        val userId = preferences[userIdKey]
        val username = preferences[usernameKey]
        val email = preferences[emailKey]
        return Triple(userId, username, email)
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
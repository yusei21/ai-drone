package com.sloth.registerapp.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenRepository private constructor(private val context: Context) {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val accessTokenFlow = MutableStateFlow(getAccessTokenBlocking())

    val token: Flow<String?> = accessTokenFlow.asStateFlow()
    val accessToken: Flow<String?> = accessTokenFlow.asStateFlow()

    suspend fun saveToken(token: String) {
        saveTokensBlocking(accessToken = token, refreshToken = getRefreshTokenBlocking())
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        saveTokensBlocking(accessToken, refreshToken)
    }

    fun saveTokensBlocking(accessToken: String, refreshToken: String?) {
        securePrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken.isNullOrBlank()) {
                    remove(KEY_REFRESH_TOKEN)
                } else {
                    putString(KEY_REFRESH_TOKEN, refreshToken)
                }
            }
            .apply()
        accessTokenFlow.value = accessToken
    }

    fun getAccessTokenBlocking(): String? =
        securePrefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshTokenBlocking(): String? =
        securePrefs.getString(KEY_REFRESH_TOKEN, null)

    suspend fun updateAccessToken(accessToken: String) {
        saveTokensBlocking(accessToken, getRefreshTokenBlocking())
    }

    suspend fun clearToken() {
        clearTokensBlocking()
    }

    suspend fun clearTokens() {
        clearTokensBlocking()
    }

    fun clearTokensBlocking() {
        securePrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        accessTokenFlow.value = null
    }

    companion object {
        private const val PREF_FILE = "secure_auth_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        @Volatile
        private var INSTANCE: TokenRepository? = null

        fun getInstance(context: Context): TokenRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TokenRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

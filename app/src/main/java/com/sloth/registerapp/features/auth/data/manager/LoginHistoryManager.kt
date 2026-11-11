package com.sloth.registerapp.features.auth.data.manager

import android.content.Context
import android.os.Build
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.database.AppDatabase
import com.sloth.registerapp.core.database.LoginHistoryEntity
import com.sloth.registerapp.features.auth.domain.model.LoginAttemptStatus
import com.sloth.registerapp.features.auth.domain.model.LoginHistory
import java.util.UUID
import kotlinx.coroutines.flow.first

class LoginHistoryManager private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val localSessionManager = LocalSessionManager.getInstance(appContext)
    private val loginHistoryDao = AppDatabase.getInstance(appContext).loginHistoryDao()

    suspend fun recordLoginAttempt(
        username: String,
        status: LoginAttemptStatus,
        ownerUserId: String? = null,
        ipOrNetwork: String? = null
    ) {
        if (username.isBlank()) return
        val resolvedOwner = ownerUserId ?: resolveOwnerUserId()
        loginHistoryDao.insert(
            LoginHistoryEntity(
                id = UUID.randomUUID().toString(),
                ownerUserId = resolvedOwner,
                usernameSnapshot = username,
                deviceLabel = deviceLabel(),
                ipOrNetwork = ipOrNetwork,
                status = status.name,
                createdAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun getRecentLogins(ownerUserId: String? = null, limit: Int = 20): List<LoginHistory> {
        val resolvedOwner = ownerUserId ?: resolveOwnerUserId()
        return loginHistoryDao.getRecentByOwner(ownerUserId = resolvedOwner, limit = limit.coerceAtLeast(1))
            .map { it.toDomain() }
    }

    private suspend fun resolveOwnerUserId(): String {
        val userId = localSessionManager.currentUserId.first()
        return if (userId.isNullOrBlank()) GUEST_OWNER_ID else userId
    }

    private fun deviceLabel(): String {
        val brand = Build.BRAND.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        return listOf(brand, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Android" }
    }

    private fun LoginHistoryEntity.toDomain(): LoginHistory {
        return LoginHistory(
            id = id,
            ownerUserId = ownerUserId,
            usernameSnapshot = usernameSnapshot,
            deviceLabel = deviceLabel,
            ipOrNetwork = ipOrNetwork,
            status = runCatching { LoginAttemptStatus.valueOf(status) }
                .getOrElse { LoginAttemptStatus.FAILED },
            createdAtMs = createdAtMs
        )
    }

    companion object {
        private const val GUEST_OWNER_ID = "__guest__"

        @Volatile
        private var INSTANCE: LoginHistoryManager? = null

        fun getInstance(context: Context): LoginHistoryManager {
            return INSTANCE ?: synchronized(this) {
                val instance = LoginHistoryManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

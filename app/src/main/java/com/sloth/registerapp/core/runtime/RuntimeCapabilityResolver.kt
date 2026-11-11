package com.sloth.registerapp.core.runtime

import android.content.Context
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.auth.model.ServerAuthState
import com.sloth.registerapp.core.database.AppDatabase
import com.sloth.registerapp.core.dji.DJIConnectionHelper
import com.sloth.registerapp.core.network.ConnectivityMonitor
import kotlinx.coroutines.flow.first

data class RuntimeCapabilities(
    val hasValidatedInternet: Boolean,
    val hasDjiConnection: Boolean,
    val hasAuthenticatedServerSession: Boolean,
    val hasMissionCache: Boolean
) {
    val canFetchWeather: Boolean
        get() = hasValidatedInternet

    val canOpenMissionControl: Boolean
        get() = hasMissionCache || hasValidatedInternet
}

class RuntimeCapabilityResolver(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityMonitor = ConnectivityMonitor.getInstance(appContext)
    private val tokenRepository = TokenRepository.getInstance(appContext)
    private val localSessionManager = LocalSessionManager.getInstance(appContext)
    private val missionCacheDao = AppDatabase.getInstance(appContext).missionCacheDao()

    suspend fun resolve(missionId: Int? = null): RuntimeCapabilities {
        val currentUserId = localSessionManager.currentUserId.first()
        val ownerUserId = if (currentUserId.isNullOrBlank()) GUEST_OWNER_ID else currentUserId
        val hasMissionCache = missionId?.let { id ->
            missionCacheDao.getById(id.toString(), ownerUserId) != null
        } ?: false

        val hasServerSession =
            !tokenRepository.getAccessTokenBlocking().isNullOrBlank() &&
                localSessionManager.serverAuthState.first() == ServerAuthState.SERVER_AUTH_OK

        return RuntimeCapabilities(
            hasValidatedInternet = connectivityMonitor.isConnectedNow(),
            hasDjiConnection = DJIConnectionHelper.getProductInstance() != null,
            hasAuthenticatedServerSession = hasServerSession,
            hasMissionCache = hasMissionCache
        )
    }

    companion object {
        private const val GUEST_OWNER_ID = "__guest__"
    }
}

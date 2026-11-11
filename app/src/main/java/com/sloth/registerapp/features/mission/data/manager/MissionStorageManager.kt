package com.sloth.registerapp.features.mission.data.manager

import android.content.Context
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.database.AppDatabase
import kotlinx.coroutines.flow.first

class MissionStorageManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val localSessionManager = LocalSessionManager.getInstance(appContext)
    private val missionCacheDao = AppDatabase.getInstance(appContext).missionCacheDao()

    suspend fun getMissionCacheSizeBytes(): Long {
        val ownerUserId = resolveOwnerUserId()
        val missions = missionCacheDao.getAll(ownerUserId)
        return missions.sumOf {
            (it.missionData.length + it.name.length + (it.description?.length ?: 0)).toLong()
        }
    }

    suspend fun clearMissionCache() {
        missionCacheDao.deleteAllByOwner(resolveOwnerUserId())
    }

    private suspend fun resolveOwnerUserId(): String {
        val userId = localSessionManager.currentUserId.first()
        return if (userId.isNullOrBlank()) GUEST_OWNER_ID else userId
    }

    companion object {
        private const val GUEST_OWNER_ID = "__guest__"

        @Volatile
        private var INSTANCE: MissionStorageManager? = null

        fun getInstance(context: Context): MissionStorageManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MissionStorageManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

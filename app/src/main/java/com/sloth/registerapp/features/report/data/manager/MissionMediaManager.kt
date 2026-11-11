package com.sloth.registerapp.features.report.data.manager

import android.content.Context
import com.sloth.registerapp.core.auth.LocalSessionManager
import com.sloth.registerapp.core.database.AppDatabase
import com.sloth.registerapp.core.database.MissionMediaEntity
import com.sloth.registerapp.features.report.domain.model.MissionMedia
import com.sloth.registerapp.features.report.domain.model.MissionMediaSource
import com.sloth.registerapp.features.report.domain.model.MissionMediaType
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MissionMediaManager private constructor(
    context: Context
) {
    private val appContext = context.applicationContext
    private val localSessionManager = LocalSessionManager.getInstance(appContext)
    private val missionMediaDao = AppDatabase.getInstance(appContext).missionMediaDao()

    suspend fun registerPhotoCapture(
        missionId: String,
        dronePath: String? = null,
        localPath: String? = null,
        source: MissionMediaSource = MissionMediaSource.DRONE_SD
    ) {
        registerMedia(
            missionId = missionId,
            mediaType = MissionMediaType.PHOTO,
            dronePath = dronePath,
            localPath = localPath,
            source = source
        )
    }

    suspend fun registerVideoCapture(
        missionId: String,
        dronePath: String? = null,
        localPath: String? = null,
        source: MissionMediaSource = MissionMediaSource.DRONE_SD
    ) {
        registerMedia(
            missionId = missionId,
            mediaType = MissionMediaType.VIDEO,
            dronePath = dronePath,
            localPath = localPath,
            source = source
        )
    }

    suspend fun getMediaByMission(missionId: String): List<MissionMedia> {
        val ownerUserId = resolveOwnerUserId()
        return missionMediaDao.getByMission(ownerUserId = ownerUserId, missionId = missionId)
            .map { it.toDomain() }
    }

    suspend fun markDownloaded(mediaId: String, localPath: String) {
        missionMediaDao.markDownloaded(id = mediaId, localPath = localPath)
    }

    private suspend fun registerMedia(
        missionId: String,
        mediaType: MissionMediaType,
        dronePath: String?,
        localPath: String?,
        source: MissionMediaSource
    ) {
        val ownerUserId = resolveOwnerUserId()
        missionMediaDao.insert(
            MissionMediaEntity(
                id = UUID.randomUUID().toString(),
                ownerUserId = ownerUserId,
                missionId = missionId,
                mediaType = mediaType.name,
                source = source.name,
                dronePath = dronePath,
                localPath = localPath,
                createdAtMs = System.currentTimeMillis(),
                sizeBytes = null,
                isDownloaded = source == MissionMediaSource.PHONE_LOCAL
            )
        )
    }

    private suspend fun resolveOwnerUserId(): String {
        val userId = localSessionManager.currentUserId.first()
        return if (userId.isNullOrBlank()) GUEST_OWNER_ID else userId
    }

    private fun MissionMediaEntity.toDomain(): MissionMedia {
        return MissionMedia(
            id = id,
            missionId = missionId,
            mediaType = runCatching { MissionMediaType.valueOf(mediaType) }.getOrElse { MissionMediaType.PHOTO },
            source = runCatching { MissionMediaSource.valueOf(source) }.getOrElse { MissionMediaSource.DRONE_SD },
            dronePath = dronePath,
            localPath = localPath,
            createdAtMs = createdAtMs,
            sizeBytes = sizeBytes,
            isDownloaded = isDownloaded
        )
    }

    companion object {
        private const val GUEST_OWNER_ID = "__guest__"

        @Volatile
        private var INSTANCE: MissionMediaManager? = null

        fun getInstance(context: Context): MissionMediaManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MissionMediaManager(context)
                INSTANCE = instance
                instance
            }
        }

        fun resolveOwnerUserIdBlocking(context: Context): String {
            val manager = LocalSessionManager.getInstance(context.applicationContext)
            return runBlocking {
                val userId = manager.currentUserId.first()
                if (userId.isNullOrBlank()) GUEST_OWNER_ID else userId
            }
        }
    }
}

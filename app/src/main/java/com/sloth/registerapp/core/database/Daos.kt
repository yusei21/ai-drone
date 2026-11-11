package com.sloth.registerapp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue WHERE status = :status AND ownerUserId = :ownerUserId ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String, ownerUserId: String): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' AND ownerUserId = :ownerUserId ORDER BY createdAt ASC")
    fun getPendingItems(ownerUserId: String): Flow<List<SyncQueueEntity>>

    @Query("UPDATE sync_queue SET status = 'PENDING' WHERE status = 'SYNCING'")
    suspend fun resetSyncingToPending()
    
    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, lastAttemptAt = :lastAttempt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, retryCount: Int, lastAttempt: Long?, errorMessage: String?)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MissionCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mission: MissionCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(missions: List<MissionCacheEntity>)

    @Query("SELECT * FROM mission_cache WHERE missionId = :missionId AND ownerUserId = :ownerUserId")
    suspend fun getById(missionId: String, ownerUserId: String): MissionCacheEntity?

    @Query("SELECT * FROM mission_cache WHERE ownerUserId = :ownerUserId ORDER BY cachedAt DESC")
    suspend fun getAll(ownerUserId: String): List<MissionCacheEntity>

    @Query("DELETE FROM mission_cache WHERE missionId = :missionId AND ownerUserId = :ownerUserId")
    suspend fun deleteById(missionId: String, ownerUserId: String)

    @Query("DELETE FROM mission_cache WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllByOwner(ownerUserId: String)
}

@Dao
interface FlightReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: FlightReportEntity)

    @Query("SELECT * FROM flight_reports WHERE ownerUserId = :ownerUserId ORDER BY startedAtMs DESC")
    suspend fun getAll(ownerUserId: String): List<FlightReportEntity>
}

@Dao
interface MissionMediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MissionMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MissionMediaEntity>)

    @Query("SELECT * FROM mission_media WHERE ownerUserId = :ownerUserId AND missionId = :missionId ORDER BY createdAtMs DESC")
    suspend fun getByMission(ownerUserId: String, missionId: String): List<MissionMediaEntity>

    @Query("SELECT * FROM mission_media WHERE ownerUserId = :ownerUserId ORDER BY createdAtMs DESC")
    suspend fun getAll(ownerUserId: String): List<MissionMediaEntity>

    @Query("UPDATE mission_media SET localPath = :localPath, isDownloaded = 1, source = 'PHONE_LOCAL' WHERE id = :id")
    suspend fun markDownloaded(id: String, localPath: String)
}

@Dao
interface LoginHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LoginHistoryEntity)

    @Query("SELECT * FROM login_history WHERE ownerUserId = :ownerUserId ORDER BY createdAtMs DESC LIMIT :limit")
    suspend fun getRecentByOwner(ownerUserId: String, limit: Int): List<LoginHistoryEntity>
}

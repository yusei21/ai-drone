package com.sloth.registerapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SyncQueueEntity::class, MissionCacheEntity::class, FlightReportEntity::class, MissionMediaEntity::class, LoginHistoryEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun missionCacheDao(): MissionCacheDao
    abstract fun flightReportDao(): FlightReportDao
    abstract fun missionMediaDao(): MissionMediaDao
    abstract fun loginHistoryDao(): LoginHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS flight_reports (
                        id TEXT NOT NULL PRIMARY KEY,
                        missionName TEXT NOT NULL,
                        aircraftName TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL,
                        startedAtMs INTEGER NOT NULL,
                        endedAtMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        finalObservation TEXT,
                        extraDataJson TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE sync_queue ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT '__guest__'"
                )
                db.execSQL(
                    "ALTER TABLE mission_cache ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT '__guest__'"
                )
                db.execSQL(
                    "ALTER TABLE flight_reports ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT '__guest__'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mission_media (
                        id TEXT NOT NULL PRIMARY KEY,
                        ownerUserId TEXT NOT NULL,
                        missionId TEXT NOT NULL,
                        mediaType TEXT NOT NULL,
                        source TEXT NOT NULL,
                        dronePath TEXT,
                        localPath TEXT,
                        createdAtMs INTEGER NOT NULL,
                        sizeBytes INTEGER,
                        isDownloaded INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS login_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        ownerUserId TEXT NOT NULL,
                        usernameSnapshot TEXT NOT NULL,
                        deviceLabel TEXT,
                        ipOrNetwork TEXT,
                        status TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drone_app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

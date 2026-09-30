package com.reddy.vittify.data.sync.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reddy.vittify.data.sync.model.SyncChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncChangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: SyncChangeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChanges(changes: List<SyncChangeEntity>): List<Long>

    @Query("SELECT * FROM sync_changes WHERE is_synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingChanges(): List<SyncChangeEntity>

    @Query("SELECT * FROM sync_changes WHERE timestamp > :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getChangesSince(sinceTimestamp: Long): List<SyncChangeEntity>

    @Query("SELECT * FROM sync_changes WHERE timestamp > :sinceTimestamp AND origin_device_id != :excludeDeviceId ORDER BY timestamp ASC")
    suspend fun getChangesSinceExcludingDevice(sinceTimestamp: Long, excludeDeviceId: String): List<SyncChangeEntity>

    @Query("UPDATE sync_changes SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT MAX(timestamp) FROM sync_changes")
    suspend fun getLatestChangeTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM sync_changes WHERE is_synced = 0")
    fun observePendingChangesCount(): Flow<Int>

    @Query("DELETE FROM sync_changes WHERE is_synced = 1 AND timestamp < :cutoffTimestamp")
    suspend fun pruneSyncedChanges(cutoffTimestamp: Long): Int

    @Query("DELETE FROM sync_changes")
    suspend fun clearAll()
}


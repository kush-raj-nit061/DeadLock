package com.ingray.deadlock.data.local.dao

import androidx.room.*
import com.ingray.deadlock.data.local.entity.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AnalyticsEventEntity): Long

    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<AnalyticsEventEntity>

    @Query("SELECT * FROM analytics_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<AnalyticsEventEntity>

    @Query("SELECT * FROM analytics_events WHERE type = :type AND timestamp >= :since")
    suspend fun getEventsByTypeSince(type: String, since: Long): List<AnalyticsEventEntity>

    @Query("DELETE FROM analytics_events WHERE timestamp < :before")
    suspend fun deleteEventsBefore(before: Long)

    @Query("SELECT COUNT(*) FROM analytics_events WHERE type = :type AND timestamp >= :since")
    suspend fun countEventsByTypeSince(type: String, since: Long): Int

    @Query("SELECT packageName, COUNT(*) as blockCount FROM analytics_events WHERE type = 'APP_BLOCKED' AND timestamp >= :since GROUP BY packageName ORDER BY blockCount DESC LIMIT :limit")
    suspend fun getMostBlockedAppsSince(since: Long, limit: Int = 10): List<PackageBlockCount>
}

data class PackageBlockCount(
    val packageName: String?,
    val blockCount: Int
)

package com.ingray.deadlock.data.local.dao

import androidx.room.*
import com.ingray.deadlock.data.local.entity.VaultedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultedNotificationDao {
    @Insert
    suspend fun insert(notification: VaultedNotificationEntity)

    @Query("SELECT * FROM vaulted_notifications WHERE isReleased = 0 ORDER BY timestamp DESC")
    fun observePendingNotifications(): Flow<List<VaultedNotificationEntity>>

    @Query("SELECT * FROM vaulted_notifications WHERE isReleased = 0")
    suspend fun getPendingNotifications(): List<VaultedNotificationEntity>

    @Query("UPDATE vaulted_notifications SET isReleased = 1 WHERE isReleased = 0")
    suspend fun markAllAsReleased()

    @Query("DELETE FROM vaulted_notifications WHERE isReleased = 1")
    suspend fun clearReleased()
}

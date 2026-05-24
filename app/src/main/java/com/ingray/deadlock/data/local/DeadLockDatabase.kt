package com.ingray.deadlock.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusScheduleDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import com.ingray.deadlock.data.local.dao.VaultedNotificationDao
import com.ingray.deadlock.data.local.entity.AnalyticsEventEntity
import com.ingray.deadlock.data.local.entity.FocusScheduleEntity
import com.ingray.deadlock.data.local.entity.FocusSessionEntity
import com.ingray.deadlock.data.local.entity.VaultedNotificationEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        AnalyticsEventEntity::class,
        FocusScheduleEntity::class,
        VaultedNotificationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DeadLockDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
    abstract fun focusScheduleDao(): FocusScheduleDao
    abstract fun vaultedNotificationDao(): VaultedNotificationDao

    companion object {
        const val DATABASE_NAME = "deadlock.db"
    }
}

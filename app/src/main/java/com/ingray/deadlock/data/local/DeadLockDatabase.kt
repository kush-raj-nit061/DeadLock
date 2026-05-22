package com.ingray.deadlock.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import com.ingray.deadlock.data.local.entity.AnalyticsEventEntity
import com.ingray.deadlock.data.local.entity.FocusSessionEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeadLockDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun analyticsEventDao(): AnalyticsEventDao

    companion object {
        const val DATABASE_NAME = "deadlock.db"
    }
}

package com.ingray.deadlock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val mode: String,
    val isActive: Boolean,
    val lockedPackages: String,      // JSON array of package names
    val distractionAttempts: Int = 0,
    val wasCompleted: Boolean = false
)

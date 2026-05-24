package com.ingray.deadlock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_schedules")
data class FocusScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: String, // Comma separated integers 1-7
    val lockedPackages: String, // Comma separated package names
    val isEnabled: Boolean = true
)

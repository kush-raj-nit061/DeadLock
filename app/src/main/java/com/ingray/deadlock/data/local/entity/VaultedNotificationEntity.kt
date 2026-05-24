package com.ingray.deadlock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaulted_notifications")
data class VaultedNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val title: String?,
    val content: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isReleased: Boolean = false
)

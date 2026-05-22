package com.ingray.deadlock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String,
    val packageName: String? = null,
    val sessionId: Long? = null,
    val timestamp: Long,
    val metadata: String? = null
)

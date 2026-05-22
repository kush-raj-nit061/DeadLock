package com.ingray.deadlock.domain.repository

import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.AnalyticsSummary
import com.ingray.deadlock.domain.model.EventType
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    suspend fun recordEvent(event: AnalyticsEvent)
    fun observeSummary(): Flow<AnalyticsSummary>
    suspend fun getSummary(): AnalyticsSummary
    suspend fun clearOldEvents(olderThanDays: Int = 90)
}

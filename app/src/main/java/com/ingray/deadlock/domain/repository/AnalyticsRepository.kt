package com.ingray.deadlock.domain.repository

import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.AnalyticsSummary
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.model.UsageStat
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    suspend fun recordEvent(event: AnalyticsEvent)
    fun observeSummary(): Flow<AnalyticsSummary>
    suspend fun getSummary(): AnalyticsSummary
    suspend fun clearOldEvents(olderThanDays: Int = 90)
    suspend fun getAppUsageStats(days: Int = 1): List<UsageStat>
    suspend fun getAppUsageStatsForDay(daysOffset: Int): List<UsageStat>
}

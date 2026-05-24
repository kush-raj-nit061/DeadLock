package com.ingray.deadlock.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import com.ingray.deadlock.data.local.entity.AnalyticsEventEntity
import com.ingray.deadlock.domain.model.*
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsDao: AnalyticsEventDao,
    private val sessionDao: FocusSessionDao
) : AnalyticsRepository {

    override suspend fun recordEvent(event: AnalyticsEvent) {
        analyticsDao.insertEvent(
            AnalyticsEventEntity(
                type = event.type.name,
                packageName = event.packageName,
                sessionId = event.sessionId,
                timestamp = event.timestamp,
                metadata = event.metadata
            )
        )
    }

    override fun observeSummary(): Flow<AnalyticsSummary> = flow {
        emit(getSummary())
    }

    override suspend fun getSummary(): AnalyticsSummary {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val weekMs = 7 * dayMs
        val todayStart = getTodayStartMs()

        val sessionsThisWeek = sessionDao.getSessionsSince(now - weekMs)
        val sessionsToday = sessionsThisWeek.filter { it.startTime >= todayStart || (it.isActive && it.endTime > todayStart) }

        val focusMinutesToday = sessionsToday
            .filter { it.wasCompleted || it.isActive }
            .sumOf { session ->
                val start = maxOf(session.startTime, todayStart)
                val end = if (session.isActive) now else minOf(session.endTime, now)
                ((end - start) / 60_000L).toInt().coerceAtLeast(0)
            }

        val focusMinutesWeek = sessionsThisWeek
            .filter { it.wasCompleted || it.isActive }
            .sumOf { session ->
                val start = maxOf(session.startTime, now - weekMs)
                val end = if (session.isActive) now else minOf(session.endTime, now)
                ((end - start) / 60_000L).toInt().coerceAtLeast(0)
            }

        val distractionCount = analyticsDao.countEventsByTypeSince(
            EventType.DISTRACTION_ATTEMPT.name, now - weekMs
        )

        val mostBlocked = analyticsDao.getMostBlockedAppsSince(now - weekMs * 4)
            .mapNotNull { it.packageName?.let { pkg -> pkg to it.blockCount } }

        val streak = calculateStreak()
        val completedSessionsWeek = sessionsThisWeek.count { it.wasCompleted }
        val score = calculateDisciplineScore(completedSessionsWeek, distractionCount, streak)

        val weeklyData = buildWeeklyData(sessionsThisWeek)
        val usageToday = getAppUsageStats(1)
        val totalScreenTimeToday = usageToday.sumOf { it.totalTimeInForeground }

        return AnalyticsSummary(
            totalFocusMinutesToday = focusMinutesToday,
            totalFocusMinutesWeek = focusMinutesWeek,
            totalScreenTimeToday = totalScreenTimeToday,
            currentStreak = streak,
            longestStreak = streak,
            disciplineScore = score,
            weeklyData = weeklyData,
            mostBlockedApps = mostBlocked,
            totalSessionsCompleted = completedSessionsWeek,
            totalDistractionAttempts = distractionCount
        )
    }

    override suspend fun clearOldEvents(olderThanDays: Int) {
        val cutoff = System.currentTimeMillis() - olderThanDays * 86_400_000L
        analyticsDao.deleteEventsBefore(cutoff)
    }

    override suspend fun getAppUsageStats(days: Int): List<UsageStat> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        
        // Use midnight as the absolute start to match "Today" view
        val startTime = if (days == 1) {
            getTodayStartMs()
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        // Use queryAndAggregateUsageStats for better accuracy on specific time ranges
        val statsMap = usageStatsManager.queryAndAggregateUsageStats(
            startTime,
            endTime
        )

        if (statsMap.isNullOrEmpty()) return emptyList()

        val pm = context.packageManager
        val launchIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = pm.queryIntentActivities(launchIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        return statsMap.values
            .filter { it.totalTimeInForeground > 0 && it.packageName in launcherApps }
            .filter { it.lastTimeStamp >= startTime } // Ensure the usage actually happened in our window
            .map { usageStat ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(usageStat.packageName, 0)).toString()
                } catch (e: Exception) {
                    usageStat.packageName.substringAfterLast('.')
                }
                UsageStat(
                    packageName = usageStat.packageName,
                    appName = appName,
                    totalTimeInForeground = usageStat.totalTimeInForeground,
                    lastTimeUsed = usageStat.lastTimeStamp
                )
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    override suspend fun getAppUsageStatsForDay(daysOffset: Int): List<UsageStat> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysOffset)
        
        // Start of selected day
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        
        // End of selected day
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        val statsMap = usageStatsManager.queryAndAggregateUsageStats(
            startTime,
            endTime
        )

        if (statsMap.isNullOrEmpty()) return emptyList()

        val pm = context.packageManager
        val launchIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = pm.queryIntentActivities(launchIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()

        return statsMap.values
            .filter { it.totalTimeInForeground > 0 && it.packageName in launcherApps }
            .filter { it.lastTimeStamp >= startTime && it.lastTimeStamp <= endTime }
            .map { usageStat ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(usageStat.packageName, 0)).toString()
                } catch (e: Exception) {
                    usageStat.packageName.substringAfterLast('.')
                }
                UsageStat(
                    packageName = usageStat.packageName,
                    appName = appName,
                    totalTimeInForeground = usageStat.totalTimeInForeground,
                    lastTimeUsed = usageStat.lastTimeStamp
                )
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    private fun getTodayStartMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private suspend fun calculateStreak(): Int {
        val sessions = sessionDao.getAllSessions()
            .filter { it.wasCompleted }
            .sortedByDescending { it.startTime }

        if (sessions.isEmpty()) return 0

        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sessionDates = sessions.map { fmt.format(Date(it.startTime)) }
            .toSortedSet()
            .toList()
            .asReversed()

        var streak = 0
        val cal = Calendar.getInstance()
        for (sessionDate in sessionDates) {
            val expected = fmt.format(cal.time)
            if (sessionDate == expected) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateDisciplineScore(
        sessionsWeek: Int,
        distractionsWeek: Int,
        streak: Int
    ): Int {
        val sessionScore = (sessionsWeek * 5).coerceAtMost(40)
        val streakScore = (streak * 3).coerceAtMost(30)
        val distractionPenalty = (distractionsWeek * 2).coerceAtMost(30)
        return (sessionScore + streakScore + 30 - distractionPenalty).coerceIn(0, 100)
    }

    private fun buildWeeklyData(sessions: List<com.ingray.deadlock.data.local.entity.FocusSessionEntity>): List<DailyStats> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = mutableMapOf<String, DailyStats>()
        val now = System.currentTimeMillis()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = fmt.format(cal.time)
            map[key] = DailyStats(key, 0, 0, 0)
        }
        for (s in sessions) {
            val key = fmt.format(Date(s.startTime))
            val existing = map[key] ?: continue
            
            val sessionDateStart = Calendar.getInstance().apply {
                time = Date(s.startTime)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val sessionDateEnd = sessionDateStart + 86_400_000L
            
            val start = maxOf(s.startTime, sessionDateStart)
            val end = if (s.isActive) minOf(now, sessionDateEnd) else minOf(s.endTime, sessionDateEnd)
            val actualMins = ((end - start) / 60_000L).toInt().coerceAtLeast(0)

            map[key] = existing.copy(
                focusMinutes = existing.focusMinutes + actualMins,
                sessionsCompleted = existing.sessionsCompleted + if (s.wasCompleted) 1 else 0,
                distractionAttempts = existing.distractionAttempts + s.distractionAttempts
            )
        }
        return map.values.sortedBy { it.date }
    }
}

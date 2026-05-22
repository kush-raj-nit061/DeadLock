package com.ingray.deadlock.data.repository

import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import com.ingray.deadlock.data.local.entity.AnalyticsEventEntity
import com.ingray.deadlock.domain.model.*
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
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
        val sessionsToday = sessionsThisWeek.filter { it.startTime >= todayStart }

        val focusMinutesToday = sessionsToday
            .filter { it.wasCompleted || it.isActive }
            .sumOf { it.durationMinutes }

        val focusMinutesWeek = sessionsThisWeek
            .filter { it.wasCompleted || it.isActive }
            .sumOf { it.durationMinutes }

        val distractionCount = analyticsDao.countEventsByTypeSince(
            EventType.DISTRACTION_ATTEMPT.name, now - weekMs
        )

        val mostBlocked = analyticsDao.getMostBlockedAppsSince(now - weekMs * 4)
            .mapNotNull { it.packageName?.let { pkg -> pkg to it.blockCount } }

        val streak = calculateStreak()
        val score = calculateDisciplineScore(sessionsThisWeek.size, distractionCount, streak)

        val weeklyData = buildWeeklyData(sessionsThisWeek)

        return AnalyticsSummary(
            totalFocusMinutesToday = focusMinutesToday,
            totalFocusMinutesWeek = focusMinutesWeek,
            currentStreak = streak,
            longestStreak = streak,
            disciplineScore = score,
            weeklyData = weeklyData,
            mostBlockedApps = mostBlocked,
            totalSessionsCompleted = sessionsThisWeek.count { it.wasCompleted },
            totalDistractionAttempts = distractionCount
        )
    }

    override suspend fun clearOldEvents(olderThanDays: Int) {
        val cutoff = System.currentTimeMillis() - olderThanDays * 86_400_000L
        analyticsDao.deleteEventsBefore(cutoff)
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
        val today = fmt.format(Date())
        val sessionDates = sessions.map { fmt.format(Date(it.startTime)) }.toSortedSet().reversed()

        var streak = 0
        val cal = Calendar.getInstance()
        val sessionDateList = sessionDates.toList()
        for (i in sessionDateList.indices) {
            val expected = fmt.format(cal.time)
            if (sessionDateList[i] == expected) {
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
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = fmt.format(cal.time)
            map[key] = DailyStats(key, 0, 0, 0)
        }
        for (s in sessions) {
            val key = fmt.format(Date(s.startTime))
            val existing = map[key] ?: continue
            map[key] = existing.copy(
                focusMinutes = existing.focusMinutes + s.durationMinutes,
                sessionsCompleted = existing.sessionsCompleted + if (s.wasCompleted) 1 else 0,
                distractionAttempts = existing.distractionAttempts + s.distractionAttempts
            )
        }
        return map.values.sortedBy { it.date }
    }
}

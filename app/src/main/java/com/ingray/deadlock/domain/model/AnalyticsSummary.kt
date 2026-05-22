package com.ingray.deadlock.domain.model

data class DailyStats(
    val date: String,           // "yyyy-MM-dd"
    val focusMinutes: Int,
    val distractionAttempts: Int,
    val sessionsCompleted: Int
)

data class AnalyticsSummary(
    val totalFocusMinutesToday: Int,
    val totalFocusMinutesWeek: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val disciplineScore: Int,     // 0–100
    val weeklyData: List<DailyStats>,
    val mostBlockedApps: List<Pair<String, Int>>,    // packageName -> count
    val totalSessionsCompleted: Int,
    val totalDistractionAttempts: Int
)

package com.ingray.deadlock.domain.model

data class FocusSession(
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val delayMinutes: Int = 0,
    val mode: FocusMode,
    val isActive: Boolean,
    val lockedPackages: List<String>,
    val isScheduled: Boolean = false,
    val scheduleId: Long? = null,
    val distractionAttempts: Int = 0,
    val wasCompleted: Boolean = false
) {
    val remainingMillis: Long
        get() = maxOf(0L, endTime - System.currentTimeMillis())

    val isLocked: Boolean
        get() {
            if (delayMinutes <= 0) return true
            val lockStartTime = startTime + (delayMinutes * 60_000L)
            return System.currentTimeMillis() >= lockStartTime
        }

    val progressFraction: Float
        get() {
            val total = endTime - startTime
            if (total <= 0L) return 1f
            val elapsed = System.currentTimeMillis() - startTime
            return (elapsed.toFloat() / total).coerceIn(0f, 1f)
        }
}

package com.ingray.deadlock.domain.model

data class FocusSession(
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val mode: FocusMode,
    val isActive: Boolean,
    val lockedPackages: List<String>,
    val distractionAttempts: Int = 0,
    val wasCompleted: Boolean = false
) {
    val remainingMillis: Long
        get() = maxOf(0L, endTime - System.currentTimeMillis())

    val progressFraction: Float
        get() {
            val total = endTime - startTime
            if (total <= 0L) return 1f
            val elapsed = System.currentTimeMillis() - startTime
            return (elapsed.toFloat() / total).coerceIn(0f, 1f)
        }
}

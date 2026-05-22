package com.ingray.deadlock.domain.model

data class LockedApp(
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val sessionId: Long,
    val lockStartTime: Long,
    val lockEndTime: Long
) {
    val isCurrentlyLocked: Boolean
        get() = System.currentTimeMillis() < lockEndTime

    val remainingMillis: Long
        get() = maxOf(0L, lockEndTime - System.currentTimeMillis())
}

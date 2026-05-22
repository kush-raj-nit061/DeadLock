package com.ingray.deadlock.domain.model

data class AnalyticsEvent(
    val id: Long = 0L,
    val type: EventType,
    val packageName: String? = null,
    val sessionId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String? = null
)

enum class EventType {
    SESSION_STARTED,
    SESSION_COMPLETED,
    SESSION_CANCELLED,
    DISTRACTION_ATTEMPT,
    UNLOCK_ATTEMPT,
    BYPASS_ATTEMPT,
    APP_BLOCKED,
    SETTINGS_NAVIGATION,
    REBOOT_RECOVERY
}

package com.ingray.deadlock.domain.model

data class UserSettings(
    val emergencyUnlockEnabled: Boolean = true,
    val emergencyUnlockCooldownMinutes: Int = 30,
    val mathChallengeEnabled: Boolean = true,
    val accountabilityEmail: String = "",
    val bedtimeLockEnabled: Boolean = false,
    val bedtimeStartHour: Int = 22,
    val bedtimeStartMinute: Int = 0,
    val bedtimeEndHour: Int = 7,
    val bedtimeEndMinute: Int = 0,
    val extendTimerOnBypassAttempt: Boolean = true,
    val extensionMinutesOnBypass: Int = 5,
    val grayscaleOnDopamineDetox: Boolean = true
)

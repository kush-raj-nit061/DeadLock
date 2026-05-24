package com.ingray.deadlock.domain.model

data class FocusSchedule(
    val id: Long = 0L,
    val name: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<Int>, // 1 (Sunday) to 7 (Saturday)
    val lockedPackages: List<String>,
    val isEnabled: Boolean = true
)

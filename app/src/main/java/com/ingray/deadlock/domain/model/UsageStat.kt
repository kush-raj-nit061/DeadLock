package com.ingray.deadlock.domain.model

data class UsageStat(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long, // in milliseconds
    val lastTimeUsed: Long
)

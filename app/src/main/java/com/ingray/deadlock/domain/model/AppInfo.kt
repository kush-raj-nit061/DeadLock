package com.ingray.deadlock.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false
)

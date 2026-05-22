package com.ingray.deadlock.domain.repository

import com.ingray.deadlock.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun saveSettings(settings: UserSettings)
}

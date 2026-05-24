package com.ingray.deadlock.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.ingray.deadlock.domain.model.UserSettings
import com.ingray.deadlock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val EMERGENCY_UNLOCK_ENABLED = booleanPreferencesKey("emergency_unlock_enabled")
        val EMERGENCY_COOLDOWN_MINUTES = intPreferencesKey("emergency_cooldown_minutes")
        val MATH_CHALLENGE_ENABLED = booleanPreferencesKey("math_challenge_enabled")
        val ACCOUNTABILITY_EMAIL = stringPreferencesKey("accountability_email")
        val BEDTIME_LOCK_ENABLED = booleanPreferencesKey("bedtime_lock_enabled")
        val BEDTIME_START_HOUR = intPreferencesKey("bedtime_start_hour")
        val BEDTIME_START_MINUTE = intPreferencesKey("bedtime_start_minute")
        val BEDTIME_END_HOUR = intPreferencesKey("bedtime_end_hour")
        val BEDTIME_END_MINUTE = intPreferencesKey("bedtime_end_minute")
        val EXTEND_ON_BYPASS = booleanPreferencesKey("extend_on_bypass")
        val EXTENSION_MINUTES = intPreferencesKey("extension_minutes")
        val GRAYSCALE_ON_DETOX = booleanPreferencesKey("grayscale_on_detox")
        val DEEP_WORK_WHITELIST = stringPreferencesKey("deep_work_whitelist")
        val MONK_MODE_WHITELIST = stringPreferencesKey("monk_mode_whitelist")
        val DOPAMINE_DETOX_BLACKLIST = stringPreferencesKey("dopamine_detox_blacklist")
        val EXAM_MODE_WHITELIST = stringPreferencesKey("exam_mode_whitelist")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    override fun observeSettings(): Flow<UserSettings> = dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    override suspend fun getSettings(): UserSettings =
        dataStore.data.first().toSettings()

    override suspend fun saveSettings(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.EMERGENCY_UNLOCK_ENABLED] = settings.emergencyUnlockEnabled
            prefs[Keys.EMERGENCY_COOLDOWN_MINUTES] = settings.emergencyUnlockCooldownMinutes
            prefs[Keys.MATH_CHALLENGE_ENABLED] = settings.mathChallengeEnabled
            prefs[Keys.ACCOUNTABILITY_EMAIL] = settings.accountabilityEmail
            prefs[Keys.BEDTIME_LOCK_ENABLED] = settings.bedtimeLockEnabled
            prefs[Keys.BEDTIME_START_HOUR] = settings.bedtimeStartHour
            prefs[Keys.BEDTIME_START_MINUTE] = settings.bedtimeStartMinute
            prefs[Keys.BEDTIME_END_HOUR] = settings.bedtimeEndHour
            prefs[Keys.BEDTIME_END_MINUTE] = settings.bedtimeEndMinute
            prefs[Keys.EXTEND_ON_BYPASS] = settings.extendTimerOnBypassAttempt
            prefs[Keys.EXTENSION_MINUTES] = settings.extensionMinutesOnBypass
            prefs[Keys.GRAYSCALE_ON_DETOX] = settings.grayscaleOnDopamineDetox
            prefs[Keys.DEEP_WORK_WHITELIST] = settings.deepWorkWhitelist.joinToString(",")
            prefs[Keys.MONK_MODE_WHITELIST] = settings.monkModeWhitelist.joinToString(",")
            prefs[Keys.DOPAMINE_DETOX_BLACKLIST] = settings.dopamineDetoxBlacklist.joinToString(",")
            prefs[Keys.EXAM_MODE_WHITELIST] = settings.examModeWhitelist.joinToString(",")
        }
    }

    override suspend fun isFirstLaunch(): Boolean =
        dataStore.data.first()[Keys.IS_FIRST_LAUNCH] ?: true

    override suspend fun setFirstLaunchCompleted() {
        dataStore.edit { it[Keys.IS_FIRST_LAUNCH] = false }
    }

    private fun Preferences.toSettings() = UserSettings(
        emergencyUnlockEnabled = this[Keys.EMERGENCY_UNLOCK_ENABLED] ?: true,
        emergencyUnlockCooldownMinutes = this[Keys.EMERGENCY_COOLDOWN_MINUTES] ?: 30,
        mathChallengeEnabled = this[Keys.MATH_CHALLENGE_ENABLED] ?: true,
        accountabilityEmail = this[Keys.ACCOUNTABILITY_EMAIL] ?: "",
        bedtimeLockEnabled = this[Keys.BEDTIME_LOCK_ENABLED] ?: false,
        bedtimeStartHour = this[Keys.BEDTIME_START_HOUR] ?: 22,
        bedtimeStartMinute = this[Keys.BEDTIME_START_MINUTE] ?: 0,
        bedtimeEndHour = this[Keys.BEDTIME_END_HOUR] ?: 7,
        bedtimeEndMinute = this[Keys.BEDTIME_END_MINUTE] ?: 0,
        extendTimerOnBypassAttempt = this[Keys.EXTEND_ON_BYPASS] ?: true,
        extensionMinutesOnBypass = this[Keys.EXTENSION_MINUTES] ?: 5,
        grayscaleOnDopamineDetox = this[Keys.GRAYSCALE_ON_DETOX] ?: true,
        deepWorkWhitelist = this[Keys.DEEP_WORK_WHITELIST]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        monkModeWhitelist = this[Keys.MONK_MODE_WHITELIST]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        dopamineDetoxBlacklist = this[Keys.DOPAMINE_DETOX_BLACKLIST]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        examModeWhitelist = this[Keys.EXAM_MODE_WHITELIST]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    )
}

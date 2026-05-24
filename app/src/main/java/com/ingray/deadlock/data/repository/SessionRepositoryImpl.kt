package com.ingray.deadlock.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import com.ingray.deadlock.data.local.entity.FocusSessionEntity
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.repository.NotificationRepository
import com.ingray.deadlock.domain.repository.ScheduleRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.domain.repository.SettingsRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: FocusSessionDao,
    private val analyticsDao: AnalyticsEventDao,
    private val scheduleRepository: Lazy<ScheduleRepository>,
    private val settingsRepository: Lazy<SettingsRepository>,
    private val notificationRepository: Lazy<NotificationRepository>
) : SessionRepository {

    private var emergencyAccessUntil: Long = 0L

    override fun observeActiveSessions(): Flow<List<FocusSession>> =
        sessionDao.observeActiveSessions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getActiveSessions(): List<FocusSession> =
        sessionDao.getActiveSessions().map { it.toDomain() }

    override suspend fun getActiveSession(): FocusSession? =
        sessionDao.getActiveSession()?.toDomain()

    override suspend fun startSession(
        durationMinutes: Int,
        delayMinutes: Int,
        mode: FocusMode,
        packageNames: List<String>,
        isScheduled: Boolean,
        scheduleId: Long?
    ): FocusSession {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes + delayMinutes) * 60_000L
        val entity = FocusSessionEntity(
            startTime = now,
            endTime = endTime,
            durationMinutes = durationMinutes,
            delayMinutes = delayMinutes,
            mode = mode.name,
            isActive = true,
            lockedPackages = packageNames.joinToString(","),
            isScheduled = isScheduled,
            scheduleId = scheduleId
        )
        val id = sessionDao.insertSession(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun cancelSession(sessionId: Long) {
        sessionDao.deactivateSession(sessionId)
        checkAndReleaseNotifications()
    }

    override suspend fun completeSession(sessionId: Long) {
        sessionDao.completeSession(sessionId)
        checkAndReleaseNotifications()
    }

    private suspend fun checkAndReleaseNotifications() {
        val active = getActiveSessions()
        if (active.isEmpty()) {
            notificationRepository.get().releaseAllNotifications()
        }
    }

    override suspend fun recordDistractionAttempt(sessionId: Long) {
        sessionDao.incrementDistractionAttempts(sessionId)
    }

    override fun observeLockedPackages(): Flow<List<String>> =
        sessionDao.observeLockedPackages().map { raw ->
            raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }

    override suspend fun isPackageLocked(packageName: String): Boolean {
        if (System.currentTimeMillis() < emergencyAccessUntil) return false
        if (isLauncher(packageName) || isKeyboard(packageName)) return false

        // 1. Check all active sessions (Live Check for Immediate Enforcement)
        val activeSessions = getActiveSessions()
        for (active in activeSessions) {
            if (System.currentTimeMillis() > active.endTime) {
                sessionDao.completeSession(active.id)
                continue
            }
            
            if (active.isLocked) {
                // Explicitly locked packages always blocked
                if (active.lockedPackages.any { it.trim() == packageName }) return true
                
                // Mode-based automated blocking
                val settings = settingsRepository.get().getSettings()
                val modeLock = when (active.mode) {
                    FocusMode.SOFT_FOCUS -> false
                    FocusMode.DEEP_WORK -> !isWorkApp(packageName) && packageName !in settings.deepWorkWhitelist
                    FocusMode.MONK_MODE -> !isEssentialApp(packageName) && packageName !in settings.monkModeWhitelist
                    FocusMode.DOPAMINE_DETOX -> isDopamineApp(packageName) || packageName in settings.dopamineDetoxBlacklist
                    FocusMode.EXAM_MODE -> !isStudyApp(packageName) && packageName !in settings.examModeWhitelist
                }
                if (modeLock) return true
            }
        }

        // 2. Check automated schedules (Live Check for Immediate Enforcement)
        val activeSchedules = scheduleRepository.get().getActiveSchedules()
        if (activeSchedules.isNotEmpty()) {
            val scheduledPackages = activeSchedules.flatMap { it.lockedPackages }.toSet()
            if (packageName in scheduledPackages) return true
        }

        return false
    }

    private fun isWorkApp(pkg: String): Boolean {
        val cat = getAppCategory(pkg)
        if (cat == ApplicationInfo.CATEGORY_PRODUCTIVITY) return true
        
        val workKeywords = listOf("calc", "note", "office", "drive", "meet", "slack", "teams", "zoom", "mail", "calendar", "todo", "code", "terminal", "intellij", "android.studio")
        return workKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isEssentialApp(pkg: String): Boolean {
        val essentialKeywords = listOf("dialer", "phone", "contact", "message", "sms", "clock", "alarm", "calendar", "telecom")
        return essentialKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isDopamineApp(pkg: String): Boolean {
        val cat = getAppCategory(pkg)
        if (cat == ApplicationInfo.CATEGORY_SOCIAL || cat == ApplicationInfo.CATEGORY_GAME || cat == ApplicationInfo.CATEGORY_VIDEO) return true
        
        val dopamineKeywords = listOf("facebook", "instagram", "twitter", "x.com", "tiktok", "snapchat", "youtube", "netflix", "primevideo", "disney", "game", "pubg", "freefire", "candy", "reels", "shorts", "twitch", "discord", "bgmi")
        return dopamineKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isStudyApp(pkg: String): Boolean {
        val cat = getAppCategory(pkg)
        if (cat == ApplicationInfo.CATEGORY_PRODUCTIVITY) return true
        
        val studyKeywords = listOf("edu", "study", "learn", "course", "dictionary", "wiki", "note", "library", "calculator", "archive", "classroom", "khan")
        return studyKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isLauncher(packageName: String): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
        }
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        return resolveInfos.any { it.activityInfo.packageName == packageName }
    }

    private fun isKeyboard(packageName: String): Boolean {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val inputMethodList = inputMethodManager.inputMethodList
        return inputMethodList.any { it.packageName == packageName }
    }

    private fun getAppCategory(packageName: String): Int {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appInfo.category
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    override suspend fun getAllSessions(): List<FocusSession> =
        sessionDao.getAllSessions().map { it.toDomain() }

    override suspend fun grantEmergencyAccess(durationMinutes: Int) {
        emergencyAccessUntil = System.currentTimeMillis() + (durationMinutes * 60_000L)
    }

    private fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        durationMinutes = durationMinutes,
        delayMinutes = delayMinutes,
        mode = FocusMode.valueOf(mode),
        isActive = isActive,
        lockedPackages = lockedPackages.split(",").filter { it.isNotBlank() },
        isScheduled = isScheduled,
        scheduleId = scheduleId,
        distractionAttempts = distractionAttempts,
        wasCompleted = wasCompleted
    )
}

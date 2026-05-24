package com.ingray.deadlock.domain.repository

import com.ingray.deadlock.domain.model.AppInfo
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.model.LockedApp
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    fun observeActiveSessions(): Flow<List<FocusSession>>

    suspend fun getActiveSessions(): List<FocusSession>

    suspend fun getActiveSession(): FocusSession?

    suspend fun startSession(
        durationMinutes: Int,
        delayMinutes: Int = 0,
        mode: FocusMode,
        packageNames: List<String>,
        isScheduled: Boolean = false,
        scheduleId: Long? = null
    ): FocusSession

    suspend fun cancelSession(sessionId: Long)

    suspend fun completeSession(sessionId: Long)

    suspend fun recordDistractionAttempt(sessionId: Long)

    fun observeLockedPackages(): Flow<List<String>>

    suspend fun isPackageLocked(packageName: String): Boolean

    suspend fun getAllSessions(): List<FocusSession>

    suspend fun grantEmergencyAccess(durationMinutes: Int = 2)
}

package com.ingray.deadlock.data.repository

import com.ingray.deadlock.data.local.dao.AnalyticsEventDao
import com.ingray.deadlock.data.local.dao.FocusSessionDao
import com.ingray.deadlock.data.local.entity.FocusSessionEntity
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: FocusSessionDao,
    private val analyticsDao: AnalyticsEventDao
) : SessionRepository {

    override fun observeActiveSession(): Flow<FocusSession?> =
        sessionDao.observeActiveSession().map { it?.toDomain() }

    override suspend fun getActiveSession(): FocusSession? =
        sessionDao.getActiveSession()?.toDomain()

    override suspend fun startSession(
        durationMinutes: Int,
        mode: FocusMode,
        packageNames: List<String>
    ): FocusSession {
        val now = System.currentTimeMillis()
        val endTime = now + durationMinutes * 60_000L
        val entity = FocusSessionEntity(
            startTime = now,
            endTime = endTime,
            durationMinutes = durationMinutes,
            mode = mode.name,
            isActive = true,
            lockedPackages = packageNames.joinToString(",")
        )
        val id = sessionDao.insertSession(entity)
        return entity.copy(id = id).toDomain()
    }

    override suspend fun cancelSession(sessionId: Long) {
        sessionDao.deactivateSession(sessionId)
    }

    override suspend fun completeSession(sessionId: Long) {
        sessionDao.completeSession(sessionId)
    }

    override suspend fun recordDistractionAttempt(sessionId: Long) {
        sessionDao.incrementDistractionAttempts(sessionId)
    }

    override fun observeLockedPackages(): Flow<List<String>> =
        sessionDao.observeLockedPackages().map { raw ->
            raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }

    override suspend fun isPackageLocked(packageName: String): Boolean {
        val active = sessionDao.getActiveSession() ?: return false
        if (System.currentTimeMillis() > active.endTime) {
            sessionDao.completeSession(active.id)
            return false
        }
        return active.lockedPackages.split(",").any { it.trim() == packageName }
    }

    override suspend fun getAllSessions(): List<FocusSession> =
        sessionDao.getAllSessions().map { it.toDomain() }

    private fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        durationMinutes = durationMinutes,
        mode = FocusMode.valueOf(mode),
        isActive = isActive,
        lockedPackages = lockedPackages.split(",").filter { it.isNotBlank() },
        distractionAttempts = distractionAttempts,
        wasCompleted = wasCompleted
    )
}

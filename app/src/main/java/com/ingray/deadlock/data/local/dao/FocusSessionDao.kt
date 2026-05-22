package com.ingray.deadlock.data.local.dao

import androidx.room.*
import com.ingray.deadlock.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    fun observeActiveSession(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun deactivateSession(sessionId: Long)

    @Query("UPDATE focus_sessions SET isActive = 0, wasCompleted = 1 WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long)

    @Query("UPDATE focus_sessions SET distractionAttempts = distractionAttempts + 1 WHERE id = :sessionId")
    suspend fun incrementDistractionAttempts(sessionId: Long)

    @Query("SELECT lockedPackages FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    fun observeLockedPackages(): Flow<String?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE wasCompleted = 1 AND startTime >= :since")
    suspend fun countCompletedSessionsSince(since: Long): Int

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :since ORDER BY startTime DESC")
    suspend fun getSessionsSince(since: Long): List<FocusSessionEntity>
}

package com.ingray.deadlock.service

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.repository.ScheduleRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
    private val sessionRepository: SessionRepository,
    private val alarmManager: ScheduleAlarmManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val allSchedules = scheduleRepository.getAllSchedules()
        val activeSchedules = scheduleRepository.getActiveSchedules()
        val currentSessions = sessionRepository.getActiveSessions()
        
        // 1. Re-calculate the next wake-up signal (ensures continuity)
        alarmManager.scheduleNextCheck(allSchedules)

        // 2. Start sessions for active schedules that don't have one
        activeSchedules.forEach { schedule ->
            val existingSession = currentSessions.find { it.scheduleId == schedule.id }
            
            val now = java.util.Calendar.getInstance()
            val end = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, schedule.endHour)
                set(java.util.Calendar.MINUTE, schedule.endMinute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            // Handle overnight schedules
            if (schedule.startHour > schedule.endHour || 
                (schedule.startHour == schedule.endHour && schedule.startMinute > schedule.endMinute)) {
                if (now.get(java.util.Calendar.HOUR_OF_DAY) >= schedule.startHour ||
                    (now.get(java.util.Calendar.HOUR_OF_DAY) == schedule.startHour && now.get(java.util.Calendar.MINUTE) >= schedule.startMinute)) {
                    end.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            } else if (end.before(now)) {
                end.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }

            val durationMinutes = ((end.timeInMillis - System.currentTimeMillis()) / 60_000).toInt()
            
            if (durationMinutes <= 0) {
                // If it's the very last minute, don't restart or update
                // The next worker run (or alarm) will clean this up
                return@forEach
            }

            if (existingSession == null) {
                sessionRepository.startSession(
                    durationMinutes = durationMinutes,
                    mode = FocusMode.SOFT_FOCUS,
                    packageNames = schedule.lockedPackages,
                    isScheduled = true,
                    scheduleId = schedule.id
                )
                startEnforcement()
            } else {
                // Check for updates
                val currentPackages = existingSession.lockedPackages.distinct().sorted()
                val targetPackages = schedule.lockedPackages.distinct().sorted()
                val currentRemaining = (existingSession.endTime - System.currentTimeMillis()) / 60_000
                val drift = Math.abs(currentRemaining - durationMinutes)

                if (currentPackages != targetPackages || drift > 2) {
                    sessionRepository.completeSession(existingSession.id)
                    sessionRepository.startSession(
                        durationMinutes = durationMinutes,
                        mode = FocusMode.SOFT_FOCUS,
                        packageNames = targetPackages,
                        isScheduled = true,
                        scheduleId = schedule.id
                    )
                }
            }
        }

        // 3. Complete sessions for schedules that are no longer active
        currentSessions.filter { it.isScheduled && it.scheduleId != null }.forEach { session ->
            if (activeSchedules.none { it.id == session.scheduleId }) {
                sessionRepository.completeSession(session.id)
            }
        }

        return Result.success()
    }

    private fun startEnforcement() {
        val intent = Intent(applicationContext, LockEnforcementService::class.java)
        applicationContext.startForegroundService(intent)
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduleWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "schedule_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<ScheduleWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

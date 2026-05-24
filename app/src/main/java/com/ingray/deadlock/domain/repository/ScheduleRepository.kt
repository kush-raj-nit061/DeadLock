package com.ingray.deadlock.domain.repository

import com.ingray.deadlock.domain.model.FocusSchedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeAllSchedules(): Flow<List<FocusSchedule>>
    suspend fun getAllSchedules(): List<FocusSchedule>
    suspend fun saveSchedule(schedule: FocusSchedule)
    suspend fun deleteSchedule(schedule: FocusSchedule)
    suspend fun getActiveSchedules(): List<FocusSchedule>
}

package com.ingray.deadlock.data.local.dao

import androidx.room.*
import com.ingray.deadlock.data.local.entity.FocusScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusScheduleDao {
    @Query("SELECT * FROM focus_schedules")
    fun observeAllSchedules(): Flow<List<FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules WHERE isEnabled = 1")
    suspend fun getEnabledSchedules(): List<FocusScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: FocusScheduleEntity): Long

    @Delete
    suspend fun deleteSchedule(schedule: FocusScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: FocusScheduleEntity)
}

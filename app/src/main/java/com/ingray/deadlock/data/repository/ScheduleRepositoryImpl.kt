package com.ingray.deadlock.data.repository

import com.ingray.deadlock.data.local.dao.FocusScheduleDao
import com.ingray.deadlock.data.local.entity.FocusScheduleEntity
import com.ingray.deadlock.domain.model.FocusSchedule
import com.ingray.deadlock.domain.repository.ScheduleRepository
import com.ingray.deadlock.service.ScheduleAlarmManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: FocusScheduleDao,
    private val alarmManager: ScheduleAlarmManager
) : ScheduleRepository {

    override fun observeAllSchedules(): Flow<List<FocusSchedule>> =
        scheduleDao.observeAllSchedules().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getAllSchedules(): List<FocusSchedule> =
        scheduleDao.observeAllSchedules().map { entities ->
            entities.map { it.toDomain() }
        }.first() // Note: This needs 'first()' from kotlinx.coroutines.flow

    override suspend fun saveSchedule(schedule: FocusSchedule) {
        scheduleDao.insertSchedule(schedule.toEntity())
        alarmManager.scheduleNextCheck(getAllSchedules())
    }

    override suspend fun deleteSchedule(schedule: FocusSchedule) {
        scheduleDao.deleteSchedule(schedule.toEntity())
        alarmManager.scheduleNextCheck(getAllSchedules())
    }

    override suspend fun getActiveSchedules(): List<FocusSchedule> {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        return scheduleDao.getEnabledSchedules().map { it.toDomain() }.filter { schedule ->
            if (currentDay !in schedule.daysOfWeek) return@filter false
            
            val startInMinutes = schedule.startHour * 60 + schedule.startMinute
            val endInMinutes = schedule.endHour * 60 + schedule.endMinute
            
            if (startInMinutes <= endInMinutes) {
                currentTimeInMinutes >= startInMinutes && currentTimeInMinutes < endInMinutes
            } else {
                // Overlaps midnight
                currentTimeInMinutes >= startInMinutes || currentTimeInMinutes < endInMinutes
            }
        }
    }

    private fun FocusScheduleEntity.toDomain(): FocusSchedule = FocusSchedule(
        id = id,
        name = name,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        daysOfWeek = daysOfWeek.split(",").filter { it.isNotBlank() }.map { it.toInt() },
        lockedPackages = lockedPackages.split(",").filter { it.isNotBlank() },
        isEnabled = isEnabled
    )

    private fun FocusSchedule.toEntity(): FocusScheduleEntity = FocusScheduleEntity(
        id = id,
        name = name,
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        daysOfWeek = daysOfWeek.joinToString(","),
        lockedPackages = lockedPackages.joinToString(","),
        isEnabled = isEnabled
    )
}

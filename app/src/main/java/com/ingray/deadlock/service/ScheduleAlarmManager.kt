package com.ingray.deadlock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ingray.deadlock.domain.model.FocusSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleAlarmManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleNextCheck(schedules: List<FocusSchedule>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Calendar.getInstance()
        
        // Find the next closest start or end time across all enabled schedules
        var nextTriggerTime: Long = Long.MAX_VALUE
        
        schedules.filter { it.isEnabled }.forEach { schedule ->
            // Check start time today
            val start = getTodayTime(schedule.startHour, schedule.startMinute)
            if (start.after(now)) {
                nextTriggerTime = minOf(nextTriggerTime, start.timeInMillis)
            }
            
            // Check end time today
            val end = getTodayTime(schedule.endHour, schedule.endMinute)
            if (end.after(now)) {
                nextTriggerTime = minOf(nextTriggerTime, end.timeInMillis)
            }
            
            // Also check tomorrow's start
            val tomorrowStart = getTodayTime(schedule.startHour, schedule.startMinute).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            nextTriggerTime = minOf(nextTriggerTime, tomorrowStart.timeInMillis)
        }

        if (nextTriggerTime != Long.MAX_VALUE) {
            val intent = Intent(context, ScheduleReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to non-exact if permission is missing
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            }
        }
    }

    private fun getTodayTime(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}

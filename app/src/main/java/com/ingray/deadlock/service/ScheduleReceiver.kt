package com.ingray.deadlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ingray.deadlock.domain.repository.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    @Inject
    lateinit var alarmManager: ScheduleAlarmManager

    override fun onReceive(context: Context, intent: Intent) {
        ScheduleWorker.runOnce(context)
        
        // Schedule next precise wakeup
        CoroutineScope(Dispatchers.IO).launch {
            val schedules = scheduleRepository.getAllSchedules()
            alarmManager.scheduleNextCheck(schedules)
        }
    }
}

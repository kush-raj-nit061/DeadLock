package com.ingray.deadlock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        CoroutineScope(Dispatchers.IO).launch {
            // Re-schedule automated disciplines
            ScheduleWorker.runOnce(context)

            val activeSession = sessionRepository.getActiveSession()
            if (activeSession != null) {
                analyticsRepository.recordEvent(
                    AnalyticsEvent(
                        type = EventType.REBOOT_RECOVERY,
                        sessionId = activeSession.id,
                        metadata = "Session restored after reboot"
                    )
                )
                // Restart the enforcement service
                val serviceIntent = Intent(context, LockEnforcementService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}

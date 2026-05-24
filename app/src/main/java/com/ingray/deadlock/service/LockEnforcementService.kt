package com.ingray.deadlock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ingray.deadlock.MainActivity
import com.ingray.deadlock.R
import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class LockEnforcementService : Service() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sessionMonitorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "deadlock_enforcement"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SESSION = "com.ingray.deadlock.STOP_SESSION"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("DeadLock Active", "Focus enforcement running"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("DeadLock Active", "Focus enforcement running"))
        }
        startSessionMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SESSION) {
            scope.launch {
                sessionRepository.getActiveSession()?.let {
                    sessionRepository.cancelSession(it.id)
                }
            }
        }
        return START_STICKY
    }

    private fun startSessionMonitor() {
        sessionMonitorJob?.cancel()
        sessionMonitorJob = scope.launch {
            while (isActive) {
                checkAndUpdateSession()
                delay(5_000L)  // check every 5 seconds
            }
        }
    }

    private suspend fun checkAndUpdateSession() {
        val session = sessionRepository.getActiveSession() ?: return
        val now = System.currentTimeMillis()

        if (now >= session.endTime) {
            sessionRepository.completeSession(session.id)
            analyticsRepository.recordEvent(
                AnalyticsEvent(
                    type = EventType.SESSION_COMPLETED,
                    sessionId = session.id
                )
            )
            updateNotification("Session Complete!", "Great work. Stay focused.")
        } else {
            val remaining = session.endTime - now
            val minutes = (remaining / 60_000).toInt()
            val seconds = ((remaining % 60_000) / 1000).toInt()
            
            val (title, content) = if (session.isLocked) {
                if (session.isScheduled) {
                    "Automated Focus Active" to "Scheduled Lockdown in progress"
                } else {
                    "DeadLock Active — ${session.mode.displayName}" to "Remaining: ${minutes}m ${seconds}s"
                }
            } else {
                val lockStartTime = session.startTime + (session.delayMinutes * 60_000L)
                val delayRemaining = lockStartTime - now
                val delayMin = (delayRemaining / 60_000).toInt()
                val delaySec = ((delayRemaining % 60_000) / 1000).toInt()
                "Locking Soon" to "App block starts in: ${delayMin}m ${delaySec}s"
            }
            
            updateNotification(title, content)
        }
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Ensure it doesn't "ping" on every timer update
            .setPriority(NotificationCompat.PRIORITY_LOW) // Keep it silent
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DeadLock Enforcement",
            NotificationManager.IMPORTANCE_LOW // Changed to LOW to prevent noise
        ).apply {
            description = "Active focus session enforcement"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

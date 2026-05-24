package com.ingray.deadlock.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ingray.deadlock.domain.repository.NotificationRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class DeadLockNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return

        scope.launch {
            if (sessionRepository.isPackageLocked(packageName)) {
                // Cancel the actual notification IMMEDIATELY before vaulting
                cancelNotification(sbn.key)

                // Intercept and vault
                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE)
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

                notificationRepository.vaultNotification(packageName, title, text)
                
                // Redundant cancel just to be absolutely sure
                cancelAllNotifications()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

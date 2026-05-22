package com.ingray.deadlock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.overlay.LockOverlayActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class DeadLockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val settingsPackages = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller"
    )

    private var lastBlockedPackage: String? = null
    private var settingsVisitCount = 0

    companion object {
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        // Ensure the enforcement service is running
        startEnforcementService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        scope.launch {
            handleForegroundApp(packageName)
        }
    }

    private suspend fun handleForegroundApp(packageName: String) {
        // Detect suspicious settings navigation
        if (packageName in settingsPackages) {
            settingsVisitCount++
            if (settingsVisitCount >= 2) {
                val session = sessionRepository.getActiveSession()
                if (session != null) {
                    analyticsRepository.recordEvent(
                        AnalyticsEvent(
                            type = EventType.BYPASS_ATTEMPT,
                            packageName = packageName,
                            sessionId = session.id
                        )
                    )
                    sessionRepository.recordDistractionAttempt(session.id)
                    settingsVisitCount = 0
                }
            }
            return
        }

        settingsVisitCount = 0

        // Check if this package is locked
        val isLocked = sessionRepository.isPackageLocked(packageName)
        if (isLocked) {
            if (packageName != lastBlockedPackage) {
                lastBlockedPackage = packageName
                val session = sessionRepository.getActiveSession()
                if (session != null) {
                    analyticsRepository.recordEvent(
                        AnalyticsEvent(
                            type = EventType.APP_BLOCKED,
                            packageName = packageName,
                            sessionId = session.id
                        )
                    )
                    analyticsRepository.recordEvent(
                        AnalyticsEvent(
                            type = EventType.DISTRACTION_ATTEMPT,
                            packageName = packageName,
                            sessionId = session.id
                        )
                    )
                    sessionRepository.recordDistractionAttempt(session.id)
                }
            }
            showLockOverlay(packageName)
        } else {
            lastBlockedPackage = null
        }
    }

    private fun showLockOverlay(blockedPackage: String) {
        val intent = Intent(applicationContext, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(LockOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        applicationContext.startActivity(intent)
    }

    private fun startEnforcementService() {
        val serviceIntent = Intent(applicationContext, LockEnforcementService::class.java)
        applicationContext.startForegroundService(serviceIntent)
    }

    override fun onInterrupt() {
        // Service interrupted — record for tamper detection
        scope.launch {
            analyticsRepository.recordEvent(
                AnalyticsEvent(type = EventType.BYPASS_ATTEMPT, metadata = "accessibility_interrupted")
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
    }
}

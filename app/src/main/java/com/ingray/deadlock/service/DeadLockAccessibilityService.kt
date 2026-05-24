package com.ingray.deadlock.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.domain.repository.SettingsRepository
import com.ingray.deadlock.overlay.LockOverlayActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class DeadLockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val installerPackages = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.settings"
    )

    private var lastBlockedPackage: String? = null

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
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val packageName = event.packageName?.toString() ?: return
            
            scope.launch {
                // 1. Check for uninstallation/disabling attempts
                if (packageName in installerPackages) {
                    checkAndBlockUninstallation(event)
                }

                // 2. Handle foreground app blocking (Improved for Immediate Enforcement)
                if (packageName != applicationContext.packageName) {
                    handleForegroundApp(packageName)
                }
            }
        }
    }

    private suspend fun checkAndBlockUninstallation(event: AccessibilityEvent) {
        val session = sessionRepository.getActiveSession() ?: return
        if (!session.isLocked) return

        val source = event.source ?: return
        
        // Find if user is looking at DeadLock's app info or settings
        val nodeText = source.findAccessibilityNodeInfosByText("DeadLock")
        val nodeTextGuard = source.findAccessibilityNodeInfosByText("Focus Guard")
        
        if (nodeText.isNotEmpty() || nodeTextGuard.isNotEmpty()) {
            // Check for dangerous buttons: "Uninstall", "Force stop", "Disable", "Deactivate", "Remove", "Off"
            val dangerousKeywords = listOf("uninstall", "force stop", "disable", "deactivate", "remove", "off", "stop")
            
            fun checkDangerousNodes(node: android.view.accessibility.AccessibilityNodeInfo?) {
                if (node == null) return
                
                val text = node.text?.toString()?.lowercase() ?: ""
                val contentDescription = node.contentDescription?.toString()?.lowercase() ?: ""
                
            if (dangerousKeywords.any { text.contains(it) || contentDescription.contains(it) }) {
                // Special check for "Off" to ensure it's a switch/toggle
                if (text == "off" && node.className != "android.widget.Switch" && node.className != "android.widget.ToggleButton") {
                    // skip if it's just random text
                } else {
                    // Go home to block the action
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showLockOverlay(applicationContext.packageName)
                    return
                }
            }
                
                for (i in 0 until node.childCount) {
                    checkDangerousNodes(node.getChild(i))
                }
            }
            
            checkDangerousNodes(source)
        }
    }

    private suspend fun handleForegroundApp(packageName: String) {
        // Essential system apps, launchers, and keyboards that should never be blocked
        if (packageName == applicationContext.packageName) return
        if (isLauncher(packageName) || isKeyboard(packageName)) return

        val essentialPackages = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.settings",
            "com.android.systemui"
        )

        if (packageName in essentialPackages) return

        // Check for active bedtime lock
        val settings = settingsRepository.getSettings()
        if (settings.bedtimeLockEnabled) {
            if (isTimeInBedtimeRange(
                    settings.bedtimeStartHour, settings.bedtimeStartMinute,
                    settings.bedtimeEndHour, settings.bedtimeEndMinute
                )
            ) {
                showLockOverlay(packageName)
                return
            }
        }

        // Detect suspicious settings navigation
        if (packageName in installerPackages) {
            // Uninstallation check is handled in onAccessibilityEvent for real-time protection
            return
        }

        // Check if this package is locked
        val isLocked = sessionRepository.isPackageLocked(packageName)
        if (isLocked) {
            if (packageName != lastBlockedPackage) {
                lastBlockedPackage = packageName
                val sessions = sessionRepository.getActiveSessions()
                sessions.filter { session ->
                    // Only record distraction for sessions that actually block this package
                    sessionRepository.isPackageLocked(packageName) && 
                    (session.lockedPackages.contains(packageName) || session.mode != FocusMode.SOFT_FOCUS)
                }.forEach { session ->
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

    private fun isTimeInBedtimeRange(startH: Int, startM: Int, endH: Int, endM: Int): Boolean {
        val now = Calendar.getInstance()
        val currentH = now.get(Calendar.HOUR_OF_DAY)
        val currentM = now.get(Calendar.MINUTE)

        val currentTime = currentH * 60 + currentM
        val startTime = startH * 60 + startM
        val endTime = endH * 60 + endM

        return if (startTime <= endTime) {
            // Same day range (e.g., 14:00 to 20:00)
            currentTime in startTime..endTime
        } else {
            // Overnight range (e.g., 22:00 to 07:00)
            currentTime >= startTime || currentTime <= endTime
        }
    }

    private fun isLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        return resolveInfos.any { it.activityInfo.packageName == packageName }
    }

    private fun isKeyboard(packageName: String): Boolean {
        val inputMethodManager = applicationContext.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val inputMethodList = inputMethodManager.inputMethodList
        return inputMethodList.any { it.packageName == packageName }
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

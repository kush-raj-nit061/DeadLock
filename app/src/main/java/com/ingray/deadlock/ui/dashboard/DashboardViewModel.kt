package com.ingray.deadlock.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AnalyticsSummary
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.model.UsageStat
import com.ingray.deadlock.data.local.entity.VaultedNotificationEntity
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.NotificationRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val activeSessions: List<FocusSession> = emptyList(),
    val summary: AnalyticsSummary? = null,
    val usageStats: List<UsageStat> = emptyList(),
    val vaultedNotifications: List<VaultedNotificationEntity> = emptyList(),
    val isUsagePermissionGranted: Boolean = true,
    val lastTick: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeData()
        initializeDefaultWhitelists()
        startTicker()
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(lastTick = System.currentTimeMillis()) }
            }
        }
    }

    private fun initializeDefaultWhitelists() {
        viewModelScope.launch {
            if (settingsRepository.isFirstLaunch()) {
                val settings = settingsRepository.getSettings()
                val pm = context.packageManager
                val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val allApps = pm.queryIntentActivities(launchIntent, 0)
                    .map { it.activityInfo.packageName }

                val deepWork = mutableListOf<String>()
                val monkMode = mutableListOf<String>()
                val dopamineDetox = mutableListOf<String>()
                val examMode = mutableListOf<String>()

                allApps.forEach { pkg ->
                    if (isLauncher(pkg) || isKeyboard(pkg)) return@forEach

                    // Deep Work Whitelist
                    if (isWorkApp(pkg)) deepWork.add(pkg)
                    // Monk Mode Whitelist
                    if (isEssentialApp(pkg)) monkMode.add(pkg)
                    // Dopamine Detox Blacklist
                    if (isDopamineApp(pkg)) dopamineDetox.add(pkg)
                    // Exam Mode Whitelist
                    if (isStudyApp(pkg)) examMode.add(pkg)
                }

                settingsRepository.saveSettings(settings.copy(
                    deepWorkWhitelist = deepWork,
                    monkModeWhitelist = monkMode,
                    dopamineDetoxBlacklist = dopamineDetox,
                    examModeWhitelist = examMode
                ))
                settingsRepository.setFirstLaunchCompleted()
            }
        }
    }

    private fun isWorkApp(pkg: String): Boolean {
        val workKeywords = listOf("calc", "note", "office", "drive", "meet", "slack", "teams", "zoom", "mail", "calendar", "todo", "code", "terminal", "intellij", "android.studio")
        return workKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isEssentialApp(pkg: String): Boolean {
        val essentialKeywords = listOf("dialer", "phone", "contact", "message", "sms", "clock", "alarm", "calendar", "telecom")
        return essentialKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isDopamineApp(pkg: String): Boolean {
        val dopamineKeywords = listOf("facebook", "instagram", "twitter", "x.com", "tiktok", "snapchat", "youtube", "netflix", "primevideo", "disney", "game", "pubg", "freefire", "candy", "reels", "shorts", "twitch", "discord", "bgmi")
        return dopamineKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isStudyApp(pkg: String): Boolean {
        val studyKeywords = listOf("edu", "study", "learn", "course", "dictionary", "wiki", "note", "library", "calculator", "archive", "classroom", "khan")
        return studyKeywords.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun isLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
        return resolveInfos.any { it.activityInfo.packageName == packageName }
    }

    private fun isKeyboard(packageName: String): Boolean {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val inputMethodList = inputMethodManager.inputMethodList
        return inputMethodList.any { it.packageName == packageName }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                sessionRepository.observeActiveSessions(),
                analyticsRepository.observeSummary(),
                notificationRepository.observeVaultedNotifications()
            ) { sessions, summary, vaulted ->
                val hasPermission = checkUsagePermission()
                val usage = if (hasPermission) analyticsRepository.getAppUsageStats(1) else emptyList()
                DashboardUiState(
                    activeSessions = sessions,
                    summary = summary,
                    usageStats = usage,
                    vaultedNotifications = vaulted,
                    isUsagePermissionGranted = hasPermission,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun releaseNotifications() {
        viewModelScope.launch {
            notificationRepository.releaseAllNotifications()
        }
    }

    private fun checkUsagePermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun refreshSummary() {
        viewModelScope.launch {
            val summary = analyticsRepository.getSummary()
            val hasPermission = checkUsagePermission()
            val usage = if (hasPermission) analyticsRepository.getAppUsageStats(1) else emptyList()
            _uiState.update { it.copy(summary = summary, usageStats = usage, isUsagePermissionGranted = hasPermission) }
        }
    }
}

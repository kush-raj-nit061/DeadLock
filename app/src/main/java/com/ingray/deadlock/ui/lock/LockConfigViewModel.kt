package com.ingray.deadlock.ui.lock

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AppInfo
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.model.FocusSession
import com.ingray.deadlock.domain.model.AnalyticsEvent
import com.ingray.deadlock.domain.model.EventType
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.service.LockEnforcementService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockConfigUiState(
    val installedApps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val durationMinutes: Int = 25,
    val delayMinutes: Int = 0,
    val isCustomDuration: Boolean = false,
    val customDurationInput: String = "",
    val isCustomDelay: Boolean = false,
    val customDelayInput: String = "",
    val focusMode: FocusMode = FocusMode.SOFT_FOCUS,
    val isLoading: Boolean = true,
    val sessionStarted: FocusSession? = null,
    val searchQuery: String = "",
    val isAccessibilityEnabled: Boolean = true,
    val isOverlayEnabled: Boolean = true,
    val isDeviceAdminEnabled: Boolean = true,
    val isExactAlarmEnabled: Boolean = true
)

@HiltViewModel
class LockConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(LockConfigUiState())
    val uiState: StateFlow<LockConfigUiState> = _uiState.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = _uiState
        .map { state ->
            val q = state.searchQuery.trim().lowercase()
            if (q.isEmpty()) state.installedApps
            else state.installedApps.filter { it.appName.lowercase().contains(q) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadInstalledApps()
        checkPermissions()
    }

    override fun onResume(owner: LifecycleOwner) {
        checkPermissions()
    }

    fun checkPermissions() {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val accessibilityEnabled = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName
        }
        val overlayEnabled = android.provider.Settings.canDrawOverlays(context)
        
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(context, com.ingray.deadlock.service.DeadLockAdminReceiver::class.java)
        val deviceAdminEnabled = dpm.isAdminActive(adminComponent)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val exactAlarmEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        _uiState.update { it.copy(
            isAccessibilityEnabled = accessibilityEnabled,
            isOverlayEnabled = overlayEnabled,
            isDeviceAdminEnabled = deviceAdminEnabled,
            isExactAlarmEnabled = exactAlarmEnabled
        ) }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pm = context.packageManager
            val apps = try {
                val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(launchIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
                }
                
                resolveInfos.map { resolve ->
                    val pkg = resolve.activityInfo.packageName
                    AppInfo(
                        packageName = pkg,
                        appName = resolve.loadLabel(pm).toString(),
                        isSystemApp = (resolve.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .distinctBy { it.packageName }
                .filter { it.packageName != context.packageName }
                .sortedBy { it.appName }
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.update { it.copy(installedApps = apps, isLoading = false) }
        }
    }

    fun toggleAppSelection(packageName: String) {
        _uiState.update { state ->
            val selected = state.selectedPackages.toMutableSet()
            if (packageName in selected) selected.remove(packageName)
            else selected.add(packageName)
            state.copy(selectedPackages = selected)
        }
    }

    fun setDuration(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes, isCustomDuration = false) }
    }

    fun toggleCustomDuration() {
        _uiState.update { it.copy(isCustomDuration = !it.isCustomDuration) }
    }

    fun setCustomDurationInput(input: String) {
        if (input.all { it.isDigit() } && input.length <= 4) {
            _uiState.update { it.copy(customDurationInput = input) }
        }
    }

    fun setDelay(minutes: Int) {
        _uiState.update { it.copy(delayMinutes = minutes, isCustomDelay = false) }
    }

    fun toggleCustomDelay() {
        _uiState.update { it.copy(isCustomDelay = !it.isCustomDelay) }
    }

    fun setCustomDelayInput(input: String) {
        if (input.all { it.isDigit() } && input.length <= 4) {
            _uiState.update { it.copy(customDelayInput = input) }
        }
    }

    fun setFocusMode(mode: FocusMode) {
        _uiState.update { it.copy(focusMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun startSession() {
        val state = _uiState.value
        if (!state.isAccessibilityEnabled || !state.isOverlayEnabled || !state.isDeviceAdminEnabled) {
            // Re-check permissions to be sure
            checkPermissions()
            return
        }

        val duration = if (state.isCustomDuration) {
            state.customDurationInput.toIntOrNull() ?: state.durationMinutes
        } else {
            state.durationMinutes
        }

        val delay = if (state.isCustomDelay) {
            state.customDelayInput.toIntOrNull() ?: state.delayMinutes
        } else {
            state.delayMinutes
        }

        if (duration <= 0) return
        if (state.selectedPackages.isEmpty() && state.focusMode == FocusMode.SOFT_FOCUS) return

        viewModelScope.launch {
            val session = sessionRepository.startSession(
                durationMinutes = duration,
                delayMinutes = delay,
                mode = state.focusMode,
                packageNames = state.selectedPackages.toList(),
                isScheduled = false
            )
            analyticsRepository.recordEvent(
                AnalyticsEvent(
                    type = EventType.SESSION_STARTED,
                    sessionId = session.id,
                    metadata = state.focusMode.name
                )
            )
            // Start enforcement service
            val serviceIntent = Intent(context, LockEnforcementService::class.java)
            context.startForegroundService(serviceIntent)
            _uiState.update { it.copy(sessionStarted = session) }
        }
    }

    fun clearSessionStarted() {
        _uiState.update { it.copy(sessionStarted = null) }
    }
}

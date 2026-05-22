package com.ingray.deadlock.ui.lock

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
    val focusMode: FocusMode = FocusMode.SOFT_FOCUS,
    val isLoading: Boolean = true,
    val sessionStarted: FocusSession? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class LockConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

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
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = context.packageManager
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
                .map { resolve ->
                    val pkg = resolve.activityInfo.packageName
                    AppInfo(
                        packageName = pkg,
                        appName = resolve.loadLabel(pm).toString(),
                        isSystemApp = (resolve.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .filter { it.packageName != context.packageName }
                .sortedBy { it.appName }

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
        _uiState.update { it.copy(durationMinutes = minutes) }
    }

    fun setFocusMode(mode: FocusMode) {
        _uiState.update { it.copy(focusMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun startSession() {
        val state = _uiState.value
        if (state.selectedPackages.isEmpty() && state.focusMode == FocusMode.SOFT_FOCUS) return

        viewModelScope.launch {
            val session = sessionRepository.startSession(
                durationMinutes = state.durationMinutes,
                mode = state.focusMode,
                packageNames = state.selectedPackages.toList()
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

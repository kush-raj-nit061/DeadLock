package com.ingray.deadlock.ui.schedule

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AppInfo
import com.ingray.deadlock.domain.model.FocusSchedule
import com.ingray.deadlock.domain.repository.ScheduleRepository
import com.ingray.deadlock.service.ScheduleWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val schedules: List<FocusSchedule> = emptyList(),
    val installedApps: List<AppInfo> = emptyList(),
    val editingSchedule: FocusSchedule? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadInstalledApps()
    }

    private fun loadData() {
        viewModelScope.launch {
            scheduleRepository.observeAllSchedules().collect { schedules ->
                _uiState.update { it.copy(schedules = schedules, isLoading = false) }
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
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
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun startEditing(schedule: FocusSchedule?) {
        _uiState.update { it.copy(editingSchedule = schedule ?: FocusSchedule(
            name = "Work Focus",
            startHour = 9, startMinute = 0,
            endHour = 17, endMinute = 0,
            daysOfWeek = listOf(2, 3, 4, 5, 6),
            lockedPackages = emptyList()
        )) }
    }

    fun stopEditing() {
        _uiState.update { it.copy(editingSchedule = null) }
    }

    fun updateEditingSchedule(update: (FocusSchedule) -> FocusSchedule) {
        _uiState.update { state ->
            state.editingSchedule?.let {
                state.copy(editingSchedule = update(it))
            } ?: state
        }
    }

    fun saveEditingSchedule() {
        val schedule = _uiState.value.editingSchedule ?: return
        viewModelScope.launch {
            scheduleRepository.saveSchedule(schedule)
            ScheduleWorker.runOnce(context)
            stopEditing()
        }
    }

    fun toggleSchedule(schedule: FocusSchedule) {
        viewModelScope.launch {
            scheduleRepository.saveSchedule(schedule.copy(isEnabled = !schedule.isEnabled))
            ScheduleWorker.runOnce(context)
        }
    }

    fun deleteSchedule(schedule: FocusSchedule) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule)
            ScheduleWorker.runOnce(context)
        }
    }

    fun createNewSchedule() {
        viewModelScope.launch {
            val newSchedule = FocusSchedule(
                name = "New Schedule",
                startHour = 9,
                startMinute = 0,
                endHour = 17,
                endMinute = 0,
                daysOfWeek = listOf(2, 3, 4, 5, 6), // Mon-Fri
                lockedPackages = emptyList()
            )
            scheduleRepository.saveSchedule(newSchedule)
        }
    }
}

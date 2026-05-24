package com.ingray.deadlock.ui.settings

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ingray.deadlock.domain.model.AppInfo
import com.ingray.deadlock.domain.model.AnalyticsSummary
import com.ingray.deadlock.domain.model.UserSettings
import com.ingray.deadlock.domain.repository.AnalyticsRepository
import com.ingray.deadlock.domain.repository.SettingsRepository
import com.ingray.deadlock.service.DeadLockAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionsState(
    val accessibilityGranted: Boolean = false,
    val usageStatsGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false,
    val notificationListenerGranted: Boolean = false
)

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val permissions: PermissionsState = PermissionsState(),
    val analyticsSummary: AnalyticsSummary? = null,
    val installedApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            analyticsRepository.observeSummary().collect { summary ->
                _uiState.update { it.copy(analyticsSummary = summary, isLoading = false) }
            }
        }
        loadInstalledApps()
        checkPermissions()
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

    override fun onResume(owner: LifecycleOwner) {
        checkPermissions()
    }

    fun checkPermissions() {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val accessibilityEnabled = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName
        }

        val usageStatsEnabled = try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }

        val overlayEnabled = Settings.canDrawOverlays(context)
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val exactAlarmEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val notificationListenerEnabled = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )?.contains(context.packageName) ?: false

        _uiState.update {
            it.copy(
                permissions = PermissionsState(
                    accessibilityGranted = accessibilityEnabled,
                    usageStatsGranted = usageStatsEnabled,
                    overlayGranted = overlayEnabled,
                    notificationsGranted = true,
                    exactAlarmGranted = exactAlarmEnabled,
                    notificationListenerGranted = notificationListenerEnabled
                )
            )
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openExactAlarmSettings(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun updateSettings(update: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val newSettings = update(_uiState.value.settings)
            settingsRepository.saveSettings(newSettings)
        }
    }
}

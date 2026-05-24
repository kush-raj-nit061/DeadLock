package com.ingray.deadlock.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.domain.model.UserSettings
import com.ingray.deadlock.ui.components.GlassCard
import com.ingray.deadlock.ui.components.NeonLabel
import com.ingray.deadlock.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Subtle ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonPurple.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(0f, size.height),
                            radius = size.width
                        ),
                        radius = size.width,
                        center = Offset(0f, size.height)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "SYSTEM",
                    fontSize = 12.sp, letterSpacing = 4.sp,
                    color = NeonPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Black
                )
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Black
                )
            }

            // Permissions
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonPurple) {
                NeonLabel("Core Authorization", NeonPurple)
                Spacer(Modifier.height(16.dp))

                val perms = listOf(
                    Triple("Accessibility Service", uiState.permissions.accessibilityGranted) {
                        viewModel.openAccessibilitySettings(context)
                    },
                    Triple("Usage Intelligence", uiState.permissions.usageStatsGranted) {
                        viewModel.openUsageAccessSettings(context)
                    },
                    Triple("Neural Overlay", uiState.permissions.overlayGranted) {
                        viewModel.openOverlaySettings(context)
                    },
                    Triple("Exact Alarms", uiState.permissions.exactAlarmGranted) {
                        viewModel.openExactAlarmSettings(context)
                    },
                    Triple("Notification Vault", uiState.permissions.notificationListenerGranted) {
                        viewModel.openNotificationListenerSettings(context)
                    }
                )

                perms.forEach { (name, granted, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (granted) NeonGreen.copy(0.05f) else SurfaceElevated)
                            .clickable { action() }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (granted) "AUTHORIZED" else "REQUIRED",
                                color = if (granted) NeonGreen else NeonRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (granted) NeonGreen.copy(0.1f) else NeonRed.copy(0.1f), CircleShape)
                                .border(1.dp, if (granted) NeonGreen.copy(0.3f) else NeonRed.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (granted) "✓" else "!", color = if (granted) NeonGreen else NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Feature Toggles
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonCyan) {
                NeonLabel("Feature Gates", NeonCyan)
                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureToggle(
                        label = "Emergency Termination",
                        enabled = uiState.settings.emergencyUnlockEnabled,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(emergencyUnlockEnabled = it) } }
                    )
                    FeatureToggle(
                        label = "Neural Math Challenge",
                        enabled = uiState.settings.mathChallengeEnabled,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(mathChallengeEnabled = it) } }
                    )
                    FeatureToggle(
                        label = "Dopamine Grayscale",
                        enabled = uiState.settings.grayscaleOnDopamineDetox,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(grayscaleOnDopamineDetox = it) } }
                    )
                    FeatureToggle(
                        label = "Extend on Bypass Attempt",
                        enabled = uiState.settings.extendTimerOnBypassAttempt,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(extendTimerOnBypassAttempt = it) } }
                    )
                    
                    HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    FeatureToggle(
                        label = "Bedtime Protocol",
                        enabled = uiState.settings.bedtimeLockEnabled,
                        onCheckedChange = { viewModel.updateSettings { s -> s.copy(bedtimeLockEnabled = it) } }
                    )

                    if (uiState.settings.bedtimeLockEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TimeSettingItem(
                                label = "WAKE",
                                hour = uiState.settings.bedtimeStartHour,
                                minute = uiState.settings.bedtimeStartMinute,
                                onTimeSelected = { h, m -> 
                                    viewModel.updateSettings { s -> s.copy(bedtimeStartHour = h, bedtimeStartMinute = m) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TimeSettingItem(
                                label = "RELEASE",
                                hour = uiState.settings.bedtimeEndHour,
                                minute = uiState.settings.bedtimeEndMinute,
                                onTimeSelected = { h, m -> 
                                    viewModel.updateSettings { s -> s.copy(bedtimeEndHour = h, bedtimeEndMinute = m) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Focus Mode Configuration
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonAmber) {
                NeonLabel("Focus Mode Logic", NeonAmber)
                Text(
                    "Customize whitelists and blacklists for specialized protocols.",
                    color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(20.dp))

                val modes = listOf(
                    FocusMode.DEEP_WORK to uiState.settings.deepWorkWhitelist,
                    FocusMode.MONK_MODE to uiState.settings.monkModeWhitelist,
                    FocusMode.DOPAMINE_DETOX to uiState.settings.dopamineDetoxBlacklist,
                    FocusMode.EXAM_MODE to uiState.settings.examModeWhitelist
                )

                modes.forEach { (mode, selection) ->
                    var expanded by remember { mutableStateOf(false) }
                    val isBlacklist = mode == FocusMode.DOPAMINE_DETOX
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .border(1.dp, if (expanded) NeonAmber.copy(0.15f) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { expanded = !expanded }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(mode.displayName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text(
                                    text = if (isBlacklist) "${selection.size} Apps Blacklisted" else "${selection.size} Apps Whitelisted",
                                    color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Text(if (expanded) "▲" else "▼", color = NeonAmber, fontSize = 12.sp)
                        }

                        if (expanded) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                            Text(
                                text = if (isBlacklist) "FORCE BLOCK LIST" else "NEVER BLOCK LIST",
                                color = if (isBlacklist) NeonRed else NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            uiState.installedApps.forEach { app ->
                                val isChecked = app.packageName in selection
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newList = if (isChecked) selection.filter { it != app.packageName }
                                            else selection + app.packageName
                                            viewModel.updateSettings { s ->
                                                when(mode) {
                                                    FocusMode.DEEP_WORK -> s.copy(deepWorkWhitelist = newList)
                                                    FocusMode.MONK_MODE -> s.copy(monkModeWhitelist = newList)
                                                    FocusMode.DOPAMINE_DETOX -> s.copy(dopamineDetoxBlacklist = newList)
                                                    FocusMode.EXAM_MODE -> s.copy(examModeWhitelist = newList)
                                                    else -> s
                                                }
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = if (isBlacklist) NeonRed else NeonGreen,
                                            checkmarkColor = ObsidianBlack,
                                            uncheckedColor = SurfaceElevated
                                        )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(app.appName, color = if (isChecked) TextPrimary else TextSecondary, fontSize = 14.sp, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Discipline Engine Calculation
            uiState.analyticsSummary?.let { summary ->
                GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonCyan) {
                    NeonLabel("Discipline Engine", NeonCyan)
                    Text("Live neural score calculation", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(20.dp))
                    
                    val weeklySessions = summary.totalSessionsCompleted
                    val currentStreak = summary.currentStreak
                    val totalDistractions = summary.totalDistractionAttempts
                    
                    val sessionPoints = (weeklySessions * 5).coerceAtMost(40)
                    val streakPoints = (currentStreak * 3).coerceAtMost(30)
                    val distractionPenalty = (totalDistractions * 2).coerceAtMost(30)
                    val basePoints = 30
                    
                    CalculationRow("Baseline Integrity", "+$basePoints", "Standard operational baseline")
                    CalculationRow("Protocol Completion", "+$sessionPoints", "$weeklySessions successful sessions")
                    CalculationRow("Consistency Chain", "+$streakPoints", "$currentStreak day sequence")
                    CalculationRow("Neural Leakage", "-$distractionPenalty", "$totalDistractions distraction attempts")
                    
                    HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "NET DISCIPLINE SCORE",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${summary.disciplineScore}/100",
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            // About
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonLabel("About Terminal")
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No Mercy v1.0\nSecure Focus Environment\nMaximum Discipline. Zero Mercy.",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureToggle(
    label: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (enabled) TextPrimary else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (enabled) FontWeight.Bold else FontWeight.Medium
        )
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NeonCyan,
                checkedThumbColor = ObsidianBlack,
                uncheckedTrackColor = SurfaceElevated,
                uncheckedThumbColor = TextTertiary
            )
        )
    }
}

@Composable
private fun TimeSettingItem(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier) {
        Text(text = label, color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceElevated)
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onTimeSelected(h, m) },
                        hour,
                        minute,
                        true
                    ).show()
                }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%02d:%02d".format(hour, minute),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    description: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = value, 
                color = if (value.startsWith("+")) NeonCyan else NeonRed,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
        Text(text = description, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

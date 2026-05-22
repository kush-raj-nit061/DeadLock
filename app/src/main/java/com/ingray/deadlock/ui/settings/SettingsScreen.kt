package com.ingray.deadlock.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

    LaunchedEffect(Unit) { viewModel.checkPermissions() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "SETTINGS",
                fontSize = 11.sp, letterSpacing = 5.sp,
                color = NeonCyan.copy(alpha = 0.6f), fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary, fontWeight = FontWeight.ExtraBold
            )

            // Permissions
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonPurple) {
                NeonLabel("Permissions", NeonPurple)
                Spacer(Modifier.height(12.dp))

                val perms = listOf(
                    Triple("Accessibility Service", uiState.permissions.accessibilityGranted) {
                        viewModel.openAccessibilitySettings(context)
                    },
                    Triple("Usage Access", uiState.permissions.usageStatsGranted) {
                        viewModel.openUsageAccessSettings(context)
                    },
                    Triple("Draw Overlay", uiState.permissions.overlayGranted) {
                        viewModel.openOverlaySettings(context)
                    }
                )

                perms.forEach { (name, granted, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (granted) NeonGreen.copy(0.08f) else NeonRed.copy(0.08f))
                            .clickable { action() }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (granted) "Enabled" else "Required",
                                color = if (granted) NeonGreen else NeonRed,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (granted) NeonGreen else NeonRed,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (granted) "✓" else "!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Feature toggles
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonCyan) {
                NeonLabel("Features", NeonCyan)
                Spacer(Modifier.height(12.dp))

                val features = listOf(
                    "Emergency Unlock" to uiState.settings.emergencyUnlockEnabled,
                    "Math Challenge" to uiState.settings.mathChallengeEnabled,
                    "Grayscale Dopamine Detox" to uiState.settings.grayscaleOnDopamineDetox,
                    "Extend on Bypass Attempt" to uiState.settings.extendTimerOnBypassAttempt,
                    "Bedtime Lock" to uiState.settings.bedtimeLockEnabled
                )

                features.forEach { (label, enabled) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = { /* Update settings */ },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = NeonCyan,
                                uncheckedTrackColor = TextSecondary.copy(alpha = 0.2f),
                                checkedThumbColor = BackgroundDeep
                            )
                        )
                    }
                }
            }

            // Emergency cooldown
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonRed) {
                NeonLabel("Emergency Cooldown", NeonRed)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.settings.emergencyUnlockCooldownMinutes} minutes",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Next unlock in: ${uiState.settings.emergencyUnlockCooldownMinutes}m",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // About
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonLabel("About")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "DeadLock v1.0\nMaximum discipline. Zero mercy.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

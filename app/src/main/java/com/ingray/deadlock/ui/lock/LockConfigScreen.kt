package com.ingray.deadlock.ui.lock

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ingray.deadlock.domain.model.FocusMode
import com.ingray.deadlock.ui.components.GlassCard
import com.ingray.deadlock.ui.components.NeonLabel
import com.ingray.deadlock.ui.theme.*

@Composable
fun LockConfigScreen(
    onSessionStarted: () -> Unit,
    viewModel: LockConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()

    LaunchedEffect(uiState.sessionStarted) {
        if (uiState.sessionStarted != null) {
            viewModel.clearSessionStarted()
            onSessionStarted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 100.dp)
        ) {
            Text(
                text = "CONFIGURE",
                fontSize = 11.sp, letterSpacing = 5.sp,
                color = NeonCyan.copy(alpha = 0.6f), fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lock Session",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary, fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(20.dp))

            // Duration picker
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonLabel("Duration")
                Spacer(Modifier.height(12.dp))
                val durations = listOf(15, 25, 45, 60, 90, 120)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.forEach { min ->
                        val selected = uiState.durationMinutes == min
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) NeonCyan.copy(0.2f)
                                    else SurfaceElevated
                                )
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setDuration(min) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${min}m",
                                fontSize = 12.sp,
                                color = if (selected) NeonCyan else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Focus mode picker
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonLabel("Focus Mode")
                Spacer(Modifier.height(12.dp))
                FocusMode.entries.forEach { mode ->
                    val selected = uiState.focusMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) NeonPurple.copy(0.15f) else Color.Transparent)
                            .clickable { viewModel.setFocusMode(mode) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = mode.displayName,
                                color = if (selected) NeonPurple else TextPrimary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            Text(
                                text = mode.description,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (selected) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(NeonPurple, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // App list
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                NeonLabel("Select Apps to Lock")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search apps...", color = TextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        cursorColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(4.dp))

            // App list — separate scrollable list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val checked = app.packageName in uiState.selectedPackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (checked) NeonCyan.copy(0.08f) else SurfaceDark)
                            .clickable { viewModel.toggleAppSelection(app.packageName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                color = if (checked) NeonCyan else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = app.packageName,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { viewModel.toggleAppSelection(app.packageName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NeonCyan,
                                uncheckedColor = TextSecondary,
                                checkmarkColor = BackgroundDeep
                            )
                        )
                    }
                }
            }
        }

        // Floating start button
        val canStart = uiState.selectedPackages.isNotEmpty() || uiState.focusMode != FocusMode.SOFT_FOCUS
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 20.dp, end = 20.dp)
        ) {
            Button(
                onClick = { viewModel.startSession() },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = BackgroundDeep,
                    disabledContainerColor = NeonCyan.copy(alpha = 0.2f),
                    disabledContentColor = TextSecondary
                )
            ) {
                Text(
                    text = "⚡  INITIATE LOCKDOWN",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontSize = 14.sp
                )
            }
        }
    }
}

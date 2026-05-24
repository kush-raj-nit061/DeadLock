package com.ingray.deadlock.ui.lock

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Intent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPermissionDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(uiState.sessionStarted) {
        if (uiState.sessionStarted != null) {
            viewModel.clearSessionStarted()
            onSessionStarted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 100.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "PROTOCOL",
                            fontSize = 12.sp, letterSpacing = 4.sp,
                            color = NeonCyan.copy(alpha = 0.6f), fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Lockdown Setup",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary, fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(12.dp))

                        if (!uiState.isAccessibilityEnabled || !uiState.isOverlayEnabled || !uiState.isDeviceAdminEnabled || !uiState.isExactAlarmEnabled) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (!uiState.isAccessibilityEnabled) {
                                        context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } else if (!uiState.isOverlayEnabled) {
                                        val intent = Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else if (!uiState.isDeviceAdminEnabled) {
                                        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                            putExtra(
                                                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                                android.content.ComponentName(context, com.ingray.deadlock.service.DeadLockAdminReceiver::class.java)
                                            )
                                            putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "No Mercy Mode requires Device Admin to prevent uninstallation.")
                                        }
                                        context.startActivity(intent)
                                    } else if (!uiState.isExactAlarmEnabled) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                            context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                        }
                                    }
                                },
                                glowColor = NeonRed
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(NeonRed.copy(alpha = 0.1f), CircleShape)
                                            .border(1.dp, NeonRed.copy(0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⚠️", fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "Critical Authorization",
                                            color = NeonRed,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                        val msg = when {
                                            !uiState.isAccessibilityEnabled -> "Accessibility access required."
                                            !uiState.isOverlayEnabled -> "Overlay permissions needed."
                                            !uiState.isDeviceAdminEnabled -> "Device Admin needed."
                                            !uiState.isExactAlarmEnabled -> "Exact Alarm permission needed."
                                            else -> ""
                                        }
                                        Text(
                                            "$msg Tap to authorize.",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // DURATION
                item {
                    ConfigGridSection(
                        title = "Focus Window",
                        options = listOf(15, 25, 45, 60, 90),
                        selectedValue = uiState.durationMinutes,
                        isCustom = uiState.isCustomDuration,
                        customValue = uiState.customDurationInput,
                        onSelect = viewModel::setDuration,
                        onToggleCustom = viewModel::toggleCustomDuration,
                        onCustomInput = viewModel::setCustomDurationInput,
                        color = NeonCyan
                    )
                }

                // DELAY
                item {
                    ConfigGridSection(
                        title = "Preparation Delay",
                        options = listOf(0, 5, 10, 20, 30),
                        selectedValue = uiState.delayMinutes,
                        isCustom = uiState.isCustomDelay,
                        customValue = uiState.customDelayInput,
                        onSelect = viewModel::setDelay,
                        onToggleCustom = viewModel::toggleCustomDelay,
                        onCustomInput = viewModel::setCustomDelayInput,
                        color = NeonAmber,
                        labelSuffix = "m"
                    )
                }

                // FOCUS MODES
                item { NeonLabel("Focus Mode", NeonPurple) }
                items(FocusMode.entries) { mode ->
                    val selected = uiState.focusMode == mode
                    val modeColor = when(mode) {
                        FocusMode.SOFT_FOCUS -> NeonCyan
                        FocusMode.DEEP_WORK -> NeonPurple
                        FocusMode.MONK_MODE -> NeonAmber
                        FocusMode.DOPAMINE_DETOX -> NeonGreen
                        FocusMode.EXAM_MODE -> Color(0xFF00A3FF)
                    }
                    
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setFocusMode(mode) },
                        glowColor = if (selected) modeColor else Color.Transparent,
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    color = if (selected) modeColor else TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = mode.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            if (selected) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(modeColor, CircleShape)
                                        .drawBehind {
                                            drawCircle(modeColor.copy(alpha = 0.3f), radius = size.width * 1.5f)
                                        }
                                )
                            }
                        }
                    }
                }

                // APPS (Only for Soft Focus)
                if (uiState.focusMode == FocusMode.SOFT_FOCUS) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonCyan) {
                            NeonLabel("App Selection", NeonCyan)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::setSearchQuery,
                                placeholder = { Text("Filter apps...", color = TextTertiary, fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = SurfaceElevated,
                                    cursorColor = NeonCyan,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceDark,
                                    unfocusedContainerColor = SurfaceDark
                                ),
                                singleLine = true
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                            }
                        }
                    } else {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val checked = app.packageName in uiState.selectedPackages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (checked) NeonCyan.copy(0.05f) else SurfaceDark)
                                    .border(
                                        width = if (checked) 1.dp else 0.dp,
                                        color = if (checked) NeonCyan.copy(0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.toggleAppSelection(app.packageName) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        color = if (checked) NeonCyan else TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = if (checked) FontWeight.Black else FontWeight.Bold
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { viewModel.toggleAppSelection(app.packageName) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = NeonCyan,
                                        uncheckedColor = TextTertiary,
                                        checkmarkColor = ObsidianBlack
                                    )
                                )
                            }
                        }
                    }
                } else {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(NeonAmber.copy(0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚙️", fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = "Rules for ${uiState.focusMode.displayName} are pre-configured in Settings.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ACTION BUTTON
            val canStart = (uiState.selectedPackages.isNotEmpty() || uiState.focusMode != FocusMode.SOFT_FOCUS)
            Button(
                onClick = {
                    if (!uiState.isAccessibilityEnabled || !uiState.isOverlayEnabled || !uiState.isDeviceAdminEnabled || !uiState.isExactAlarmEnabled) {
                        showPermissionDialog = true
                    } else {
                        viewModel.startSession()
                    }
                },
                enabled = canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = ObsidianBlack,
                    disabledContainerColor = SurfaceVariant,
                    disabledContentColor = TextTertiary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = "INITIATE LOCKDOWN",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 15.sp
                )
            }
        }

        if (showPermissionDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showPermissionDialog = false }) {
                GlassCard(glowColor = NeonRed) {
                    NeonLabel("Setup Required", NeonRed)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "To enforce 'No Mercy' mode, you must authorize all system permissions.",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            showPermissionDialog = false
                            if (!uiState.isAccessibilityEnabled) {
                                context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } else if (!uiState.isOverlayEnabled) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else if (!uiState.isDeviceAdminEnabled) {
                                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(
                                        android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                        android.content.ComponentName(context, com.ingray.deadlock.service.DeadLockAdminReceiver::class.java)
                                    )
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "No Mercy Mode requires Device Admin.")
                                }
                                context.startActivity(intent)
                            } else {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                    ) {
                        Text("Open System Settings", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigGridSection(
    title: String,
    options: List<Int>,
    selectedValue: Int,
    isCustom: Boolean,
    customValue: String,
    onSelect: (Int) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomInput: (String) -> Unit,
    color: Color,
    labelSuffix: String = "m"
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = color) {
        NeonLabel(title, color)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { val_ ->
                val selected = selectedValue == val_ && !isCustom
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) color.copy(0.2f) else SurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (selected) color else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(val_) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (val_ == 0 && labelSuffix == "m") "None" else "${val_}$labelSuffix",
                        fontSize = 13.sp,
                        color = if (selected) color else TextSecondary,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
            
            val customActive = isCustom
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (customActive) color.copy(0.2f) else SurfaceElevated)
                    .border(
                        width = 1.dp,
                        color = if (customActive) color else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onToggleCustom() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Edit",
                    fontSize = 13.sp,
                    color = if (customActive) color else TextSecondary,
                    fontWeight = if (customActive) FontWeight.Black else FontWeight.Bold
                )
            }
        }

        if (isCustom) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = customValue,
                onValueChange = onCustomInput,
                label = { Text("Custom minutes", color = TextTertiary, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = SurfaceElevated,
                    cursorColor = color,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                singleLine = true
            )
        }
    }
}

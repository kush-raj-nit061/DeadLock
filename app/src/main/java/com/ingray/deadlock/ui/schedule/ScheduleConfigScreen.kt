package com.ingray.deadlock.ui.schedule

import android.app.TimePickerDialog
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ingray.deadlock.domain.model.FocusSchedule
import com.ingray.deadlock.ui.components.GlassCard
import com.ingray.deadlock.ui.components.NeonLabel
import com.ingray.deadlock.ui.theme.*

@Composable
fun ScheduleConfigScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var scheduleToDelete by remember { mutableStateOf<FocusSchedule?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Ambient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width, size.height * 0.8f),
                            radius = size.width
                        ),
                        radius = size.width,
                        center = Offset(size.width, size.height * 0.8f)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 100.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "AUTOMATION",
                    fontSize = 12.sp, letterSpacing = 4.sp,
                    color = NeonCyan.copy(alpha = 0.6f), fontWeight = FontWeight.Black
                )
                Text(
                    text = "Chronos Rules",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(24.dp))

            // DAILY TIMELINE VISUAL
            TimelineSummary(uiState.schedules)
            
            Spacer(Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(uiState.schedules) { schedule ->
                    ObsidianScheduleItem(
                        schedule = schedule,
                        onToggle = { viewModel.toggleSchedule(schedule) },
                        onDelete = { scheduleToDelete = schedule },
                        onClick = { viewModel.startEditing(schedule) }
                    )
                }
                
                item {
                    Button(
                        onClick = { viewModel.startEditing(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
                    ) {
                        Text("+ Create Logic Gate", color = NeonCyan, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }

        if (uiState.editingSchedule != null) {
            EditScheduleDialog(
                schedule = uiState.editingSchedule!!,
                installedApps = uiState.installedApps,
                onDismiss = { viewModel.stopEditing() },
                onSave = { viewModel.saveEditingSchedule() },
                onUpdate = { viewModel.updateEditingSchedule(it) }
            )
        }

        if (scheduleToDelete != null) {
            DeleteConfirmationDialog(
                name = scheduleToDelete?.name ?: "",
                onConfirm = {
                    scheduleToDelete?.let { viewModel.deleteSchedule(it) }
                    scheduleToDelete = null
                },
                onDismiss = { scheduleToDelete = null }
            )
        }
    }
}

@Composable
fun TimelineSummary(schedules: List<FocusSchedule>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        NeonLabel("Active Windows", NeonCyan)
        Spacer(Modifier.height(16.dp))
        
        // 24 Hour Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .drawBehind {
                    schedules.filter { it.isEnabled }.forEach { schedule ->
                        val startPos = (schedule.startHour * 60 + schedule.startMinute).toFloat() / (24 * 60)
                        val endPos = (schedule.endHour * 60 + schedule.endMinute).toFloat() / (24 * 60)
                        
                        if (startPos < endPos) {
                            drawRoundRect(
                                color = NeonCyan,
                                topLeft = Offset(size.width * startPos, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width * (endPos - startPos), size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                            )
                        } else {
                            // Midnight overlap
                            drawRoundRect(
                                color = NeonCyan,
                                topLeft = Offset(size.width * startPos, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width * (1f - startPos), size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                            )
                            drawRoundRect(
                                color = NeonCyan,
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width * endPos, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                            )
                        }
                    }
                }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("12:00", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("23:59", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ObsidianScheduleItem(
    schedule: FocusSchedule,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val accent = if (schedule.isEnabled) NeonCyan else TextTertiary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(
                width = 1.dp,
                color = if (schedule.isEnabled) NeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name, 
                    color = if (schedule.isEnabled) TextPrimary else TextTertiary, 
                    fontWeight = FontWeight.Black, 
                    fontSize = 18.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "%02d:%02d — %02d:%02d".format(schedule.startHour, schedule.startMinute, schedule.endHour, schedule.endMinute),
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(4.dp).background(TextTertiary, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${schedule.lockedPackages.size} Apps",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = schedule.daysOfWeek.sorted().joinToString(" ") { 
                        when(it) {
                            2 -> "M"
                            3 -> "T"
                            4 -> "W"
                            5 -> "T"
                            6 -> "F"
                            7 -> "S"
                            1 -> "S"
                            else -> ""
                        }
                    },
                    color = TextTertiary,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = NeonCyan,
                        checkedThumbColor = ObsidianBlack,
                        uncheckedTrackColor = SurfaceElevated
                    )
                )
                IconButton(onClick = onDelete, modifier = Modifier.padding(start = 8.dp)) {
                    Text("🗑️", fontSize = 18.sp, color = NeonRed.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun EditScheduleDialog(
    schedule: FocusSchedule,
    installedApps: List<com.ingray.deadlock.domain.model.AppInfo>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdate: ((FocusSchedule) -> FocusSchedule) -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(),
            glowColor = NeonCyan,
            cornerRadius = 28.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NeonLabel("Configure Protocol", NeonCyan)
                Spacer(Modifier.height(20.dp))
                
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item {
                        OutlinedTextField(
                            value = schedule.name,
                            onValueChange = { n -> onUpdate { it.copy(name = n) } },
                            label = { Text("Protocol Name", color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = SurfaceElevated,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Start Time
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WAKE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Button(
                                    onClick = {
                                        TimePickerDialog(context, { _, h, m -> 
                                            onUpdate { it.copy(startHour = h, startMinute = m) }
                                        }, schedule.startHour, schedule.startMinute, true).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                                ) {
                                    Text("%02d:%02d".format(schedule.startHour, schedule.startMinute), color = TextPrimary, fontWeight = FontWeight.Black)
                                }
                            }
                            // End Time
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RELEASE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Button(
                                    onClick = {
                                        TimePickerDialog(context, { _, h, m -> 
                                            onUpdate { it.copy(endHour = h, endMinute = m) }
                                        }, schedule.endHour, schedule.endMinute, true).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                                ) {
                                    Text("%02d:%02d".format(schedule.endHour, schedule.endMinute), color = TextPrimary, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text("REPETITION", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val days = listOf(2, 3, 4, 5, 6, 7, 1) // Mon-Sun
                                days.forEach { day ->
                                    val selected = day in schedule.daysOfWeek
                                    val label = when(day) {
                                        1 -> "S"; 2 -> "M"; 3 -> "T"; 4 -> "W"; 5 -> "T"; 6 -> "F"; 7 -> "S"
                                        else -> ""
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) NeonCyan else SurfaceElevated)
                                            .border(1.dp, if (selected) NeonCyan else Color.Transparent, CircleShape)
                                            .clickable {
                                                val newList = if (selected) schedule.daysOfWeek.filter { it != day }
                                                else schedule.daysOfWeek + day
                                                onUpdate { it.copy(daysOfWeek = newList) }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (selected) ObsidianBlack else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        NeonLabel("Terminal Targets", NeonRed)
                        Text("Apps to be strictly blocked during this protocol", color = TextTertiary, fontSize = 11.sp)
                    }

                    items(installedApps) { app ->
                        val checked = app.packageName in schedule.lockedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (checked) NeonRed.copy(0.05f) else Color.Transparent)
                                .clickable {
                                    val newList = if (checked) schedule.lockedPackages.filter { it != app.packageName }
                                    else schedule.lockedPackages + app.packageName
                                    onUpdate { it.copy(lockedPackages = newList) }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    val newList = if (checked) schedule.lockedPackages.filter { it != app.packageName }
                                    else schedule.lockedPackages + app.packageName
                                    onUpdate { it.copy(lockedPackages = newList) }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = NeonRed, checkmarkColor = ObsidianBlack)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(app.appName, color = if (checked) TextPrimary else TextSecondary, fontSize = 14.sp, fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("SAVE PROTOCOL", color = ObsidianBlack, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminate Protocol?", color = TextPrimary, fontWeight = FontWeight.Black) },
        text = { Text("Are you sure you want to delete '$name'? This neural link cannot be restored.", color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("TERMINATE", color = NeonRed, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextPrimary)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp)
    )
}

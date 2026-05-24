package com.ingray.deadlock.ui.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ingray.deadlock.ui.components.*
import com.ingray.deadlock.ui.theme.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // High-end ambient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonGreen.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = size.width
                        ),
                        radius = size.width,
                        center = Offset(0f, 0f)
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
                    text = "INTELLIGENCE",
                    fontSize = 12.sp, letterSpacing = 4.sp,
                    color = NeonGreen.copy(alpha = 0.6f), fontWeight = FontWeight.Black
                )
                Text(
                    text = "Discipline Metrics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Black
                )
            }

            uiState.summary?.let { summary ->
                // VITAL STATS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val screenTimeHours = summary.totalScreenTimeToday / (1000 * 60 * 60)
                    val screenTimeMinutes = (summary.totalScreenTimeToday / (1000 * 60)) % 60
                    
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Overall",
                        value = if (screenTimeHours > 0) "${screenTimeHours}h ${screenTimeMinutes}m" else "${screenTimeMinutes}m",
                        color = NeonPurple,
                        icon = "📱"
                    )
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Focus",
                        value = "${summary.totalFocusMinutesWeek / 60}h",
                        color = NeonCyan,
                        icon = "⚡"
                    )
                }

                // 7-DAY TREND
                if (summary.weeklyData.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonCyan) {
                        NeonLabel("7-Day Focus Trend", NeonCyan)
                        Spacer(Modifier.height(24.dp))
                        
                        val chartData = summary.weeklyData.map { day ->
                            val label = day.date.takeLast(2)
                            label to day.focusMinutes
                        }
                        val maxVal = chartData.maxOfOrNull { it.second } ?: 1
                        
                        MiniBarChart(
                            data = chartData,
                            maxValue = maxVal,
                            barColor = NeonCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }

                // DATE SELECTOR & USAGE HEATMAP
                GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonPurple) {
                    NeonLabel("Usage Intelligence", NeonPurple)
                    Spacer(Modifier.height(16.dp))
                    
                    // Horizontal obsidian selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..6) {
                            val selected = uiState.selectedDateIndex == i
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) NeonPurple.copy(0.15f) else SurfaceElevated)
                                    .border(1.dp, if (selected) NeonPurple.copy(0.3f) else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable { viewModel.selectDate(i) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = when(i) {
                                        0 -> "Today"
                                        1 -> "Yesterday"
                                        else -> {
                                            val cal = java.util.Calendar.getInstance()
                                            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                                            java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(cal.time)
                                        }
                                    },
                                    color = if (selected) NeonPurple else TextTertiary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonPurple, strokeWidth = 2.dp)
                        }
                    } else if (uiState.dailyUsageStats.isEmpty()) {
                        Text("No logs found for this period.", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    } else {
                        uiState.dailyUsageStats.take(8).forEach { stat ->
                            val hours = stat.totalTimeInForeground / (1000 * 60 * 60)
                            val minutes = (stat.totalTimeInForeground / (1000 * 60)) % 60
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stat.appName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(stat.packageName, color = TextTertiary, fontSize = 10.sp)
                                }
                                Text(
                                    text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                                    color = NeonPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            HorizontalDivider(color = SurfaceVariant, thickness = 0.5.dp)
                        }
                    }
                }

                // STREAK RECOGNITION
                GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonAmber) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            NeonLabel("Discipline Streak", NeonAmber)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${summary.currentStreak} Days",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonAmber,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "Peak: ${summary.longestStreak}d",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(NeonAmber.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, NeonAmber.copy(0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 32.sp)
                        }
                    }
                }

                // TOP OBSTACLES
                if (summary.mostBlockedApps.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonRed) {
                        NeonLabel("Terminal Obstacles", NeonRed)
                        Text("Top 5 apps prevented this month", color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(20.dp))
                        summary.mostBlockedApps.take(5).forEachIndexed { idx, (pkg, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(SurfaceElevated, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = NeonRed
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        pkg.substringAfterLast('.'),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "$count BLOCKS",
                                    color = NeonRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
fun ModernStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    icon: String
) {
    GlassCard(
        modifier = modifier,
        glowColor = color,
        cornerRadius = 24.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = (-1).sp
            )
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

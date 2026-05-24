package com.ingray.deadlock.ui.dashboard

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ingray.deadlock.ui.components.*
import com.ingray.deadlock.ui.theme.*

@Composable
fun DashboardScreen(
    onStartFocus: () -> Unit,
    onViewSession: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSummary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) { viewModel.refreshSummary() }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // High-end ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(size.width * 0.8f, 0f),
                            radius = size.width
                        ),
                        radius = size.width,
                        center = Offset(size.width * 0.8f, 0f)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "VITAL CORE",
                        fontSize = 12.sp,
                        letterSpacing = 4.sp,
                        color = NeonCyan.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "No Mercy",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "v1.0",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // THE CORE: Discipline Score Nebula
            uiState.summary?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DisciplineScoreNebula(it.disciplineScore)
                }
            }

            // Usage Permission Warning
            if (!uiState.isUsagePermissionGranted) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    },
                    glowColor = NeonAmber
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Usage Access Needed",
                                color = NeonAmber,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Tap to enable Digital Wellbeing tracking.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Active session banners
            if (uiState.activeSessions.isNotEmpty()) {
                uiState.activeSessions.forEach { session ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewSession() },
                        glowColor = NeonGreen
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                NeonLabel(if (session.isScheduled) "Auto Protocol" else "Active Focus", NeonGreen)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = session.mode.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black
                                )
                                val rem = session.remainingMillis
                                val m = (rem / 60_000).toInt()
                                val s = ((rem % 60_000) / 1000).toInt()
                                Text(
                                    text = "%02d:%02d remaining".format(m, s),
                                    color = NeonGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(NeonGreen.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, NeonGreen.copy(0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▶", fontSize = 24.sp, color = NeonGreen)
                            }
                        }
                    }
                }
            } else {
                // START PROTOCOL CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SurfaceElevated, SurfaceVariant)
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(NeonCyan.copy(0.4f), Color.Transparent)),
                            RoundedCornerShape(28.dp)
                        )
                        .clickable { onStartFocus() }
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NeonCyan.copy(0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 32.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Initiate Lockdown",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Activate focused discipline now.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // VITAL STATS GRID
            uiState.summary?.let { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val screenTimeHours = summary.totalScreenTimeToday / (1000 * 60 * 60)
                    val screenTimeMinutes = (summary.totalScreenTimeToday / (1000 * 60)) % 60
                    
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Screen",
                        value = if (screenTimeHours > 0) "${screenTimeHours}h ${screenTimeMinutes}m" else "${screenTimeMinutes}m",
                        color = NeonPurple,
                        icon = "📱"
                    )
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Focus",
                        value = "${summary.totalFocusMinutesToday}m",
                        color = NeonCyan,
                        icon = "⚡"
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Streak",
                        value = "${summary.currentStreak}d",
                        color = NeonAmber,
                        icon = "🔥"
                    )
                    ModernStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Blocked",
                        value = "${summary.totalDistractionAttempts}",
                        color = NeonRed,
                        icon = "🚫"
                    )
                }

                // Notification Vault Section
                if (uiState.vaultedNotifications.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonAmber) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                NeonLabel("Notification Vault", NeonAmber)
                                Text(
                                    text = "${uiState.vaultedNotifications.size} Alerts Intercepted",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Button(
                                onClick = { viewModel.releaseNotifications() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber.copy(0.1f), contentColor = NeonAmber),
                                border = BorderStroke(1.dp, NeonAmber.copy(0.3f))
                            ) {
                                Text("RELEASE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        uiState.vaultedNotifications.take(3).forEach { vault ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(NeonAmber, CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(vault.title ?: "Notification", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(vault.packageName.substringAfterLast('.'), color = TextTertiary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Digital Pulse Section
                if (uiState.usageStats.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonPurple) {
                        NeonLabel("Digital Pulse", NeonPurple)
                        Text(
                            text = "Today's usage breakdown",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        
                        val maxUsage = uiState.usageStats.maxOfOrNull { it.totalTimeInForeground } ?: 1L
                        
                        uiState.usageStats.take(4).forEach { stat ->
                            val hours = stat.totalTimeInForeground / (1000 * 60 * 60)
                            val minutes = (stat.totalTimeInForeground / (1000 * 60)) % 60
                            
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stat.appName,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                                        color = NeonPurple,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceElevated)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(stat.totalTimeInForeground.toFloat() / maxUsage)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(NeonPurple, NeonPurple.copy(alpha = 0.4f))
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
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
        glowColor = color
    ) {
        Column {
            Text(text = icon, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color,
                letterSpacing = (-1).sp
            )
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = TextSecondary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

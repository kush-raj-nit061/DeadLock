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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ingray.deadlock.ui.components.*
import com.ingray.deadlock.ui.theme.*

@Composable
fun DashboardScreen(
    onStartFocus: () -> Unit,
    onViewSession: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshSummary() }

    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "bg_pulse")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient glow background
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-100).dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(listOf(NeonCyan.copy(alpha = bgAlpha), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DEADLOCK",
                        fontSize = 11.sp,
                        letterSpacing = 5.sp,
                        color = NeonCyan.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Focus Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                uiState.summary?.let {
                    DisciplineScoreRing(score = it.disciplineScore)
                }
            }

            // Active session banner
            val session = uiState.activeSession
            if (session != null && session.isActive) {
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
                        Column {
                            NeonLabel("Active Session", NeonGreen)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = session.mode.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            val rem = session.remainingMillis
                            val m = (rem / 60_000).toInt()
                            val s = ((rem % 60_000) / 1000).toInt()
                            Text(
                                text = "%02d:%02d remaining".format(m, s),
                                color = NeonGreen,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(NeonGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", fontSize = 18.sp, color = NeonGreen)
                        }
                    }
                }
            } else {
                // Start focus CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan.copy(0.15f), NeonPurple.copy(0.1f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(NeonCyan.copy(0.5f), NeonPurple.copy(0.3f))),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onStartFocus() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚡", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Start Focus Session",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lock distractions. Activate discipline.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Today stats
            uiState.summary?.let { summary ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    NeonLabel("Today's Stats")
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Focus Min",
                            value = "${summary.totalFocusMinutesToday}",
                            valueColor = NeonCyan
                        )
                        StatItem(
                            label = "Streak",
                            value = "${summary.currentStreak}d",
                            valueColor = NeonAmber
                        )
                        StatItem(
                            label = "Sessions",
                            value = "${summary.totalSessionsCompleted}",
                            valueColor = NeonGreen
                        )
                        StatItem(
                            label = "Blocked",
                            value = "${summary.totalDistractionAttempts}",
                            valueColor = NeonRed
                        )
                    }
                }

                // Weekly chart
                if (summary.weeklyData.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        NeonLabel("7-Day Focus")
                        Spacer(Modifier.height(16.dp))
                        val chartData = summary.weeklyData.map { day ->
                            val label = day.date.takeLast(5).replace("-", "/")
                            label to day.focusMinutes
                        }
                        val maxVal = chartData.maxOfOrNull { it.second } ?: 1
                        MiniBarChart(
                            data = chartData,
                            maxValue = maxVal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                    }
                }

                // Top blocked apps
                if (summary.mostBlockedApps.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonRed) {
                        NeonLabel("Most Blocked Apps", NeonRed)
                        Spacer(Modifier.height(12.dp))
                        summary.mostBlockedApps.take(5).forEach { (pkg, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = pkg.substringAfterLast('.'),
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$count blocks",
                                    color = NeonRed.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

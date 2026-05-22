package com.ingray.deadlock.ui.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ingray.deadlock.ui.components.DisciplineScoreRing
import com.ingray.deadlock.ui.components.GlassCard
import com.ingray.deadlock.ui.components.MiniBarChart
import com.ingray.deadlock.ui.components.NeonLabel
import com.ingray.deadlock.ui.components.StatItem
import com.ingray.deadlock.ui.theme.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val scrollState = rememberScrollState()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = EaseInOutSine),
            RepeatMode.Reverse
        ), label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Box(
            modifier = Modifier
                .size(500.dp)
                .align(Alignment.TopStart)
                .offset(x = (-150).dp, y = (-100).dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(NeonGreen.copy(alpha = bgAlpha), Color.Transparent)
                    ),
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
            Text(
                text = "ANALYTICS",
                fontSize = 11.sp, letterSpacing = 5.sp,
                color = NeonGreen.copy(alpha = 0.6f), fontWeight = FontWeight.Bold
            )
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary, fontWeight = FontWeight.ExtraBold
            )

            uiState.summary?.let { summary ->
                // Discipline score
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    DisciplineScoreRing(score = summary.disciplineScore)
                }

                // Key stats
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    NeonLabel("This Week")
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Focus Hours",
                            value = "${summary.totalFocusMinutesWeek / 60}",
                            valueColor = NeonCyan
                        )
                        StatItem(
                            label = "Sessions",
                            value = "${summary.totalSessionsCompleted}",
                            valueColor = NeonGreen
                        )
                        StatItem(
                            label = "Distraction",
                            value = "${summary.totalDistractionAttempts}",
                            valueColor = NeonRed
                        )
                    }
                }

                // Weekly chart
                if (summary.weeklyData.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        NeonLabel("7-Day Trend")
                        Spacer(Modifier.height(16.dp))
                        val chartData = summary.weeklyData.map { day ->
                            val label = day.date.takeLast(5).replace("-", "/")
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

                // Streak
                GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonAmber) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            NeonLabel("Current Streak", NeonAmber)
                            Text(
                                text = "${summary.currentStreak}d",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonAmber
                            )
                            Text(
                                text = "Keep it up!",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(NeonAmber.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 32.sp)
                        }
                    }
                }

                // Top distractions
                if (summary.mostBlockedApps.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonRed) {
                        NeonLabel("Top Distractions", NeonRed)
                        Spacer(Modifier.height(12.dp))
                        summary.mostBlockedApps.forEachIndexed { idx, (pkg, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(NeonRed.copy(0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonRed
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        pkg.substringAfterLast('.'),
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    "$count",
                                    color = NeonRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
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
                    Text("Loading analytics...", color = TextSecondary)
                }
            }
        }
    }
}

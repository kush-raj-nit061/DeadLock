package com.ingray.deadlock.ui.session

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.ingray.deadlock.ui.components.GlassCard
import com.ingray.deadlock.ui.components.NeonLabel
import com.ingray.deadlock.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FocusSessionScreen(
    onSessionEnded: () -> Unit,
    viewModel: FocusSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSessionEnded) {
        if (uiState.isSessionEnded) onSessionEnded()
    }

    // Tick remaining time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            viewModel.updateRemainingTime()
        }
    }

    val session = uiState.sessions.getOrNull(uiState.currentSessionIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.Center
    ) {
        // High-end ambient background glow (Pulse based on session)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width
                        ),
                        radius = size.width,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Switcher
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (uiState.sessions.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        uiState.sessions.forEachIndexed { index, s ->
                            val selected = uiState.currentSessionIndex == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (selected) NeonCyan.copy(0.1f) else SurfaceElevated)
                                    .border(
                                        width = if (selected) 1.dp else 0.dp,
                                        color = if (selected) NeonCyan.copy(0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable { viewModel.selectSession(index) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (s.isScheduled) "Auto Protocol" else "Manual Core",
                                    color = if (selected) NeonCyan else TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                if (session != null) {
                    Text(
                        text = "FOCUS ACTIVE",
                        fontSize = 13.sp, letterSpacing = 6.sp,
                        color = NeonCyan.copy(alpha = 0.6f), fontWeight = FontWeight.Black
                    )
                    Text(
                        text = session.mode.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary, fontWeight = FontWeight.Black
                    )
                }
            }

            if (session != null) {
                // THE CHRONO: Breathing Progress Ring
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = session.progressFraction
                    // Keying by lastTick ensures animation/values update even if progressFraction rounded value stays same
                    key(uiState.lastTick) {
                        BreathingRing(progress = progress)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val rem = session.remainingMillis
                        val h = rem / 3_600_000
                        val m = (rem % 3_600_000) / 60_000
                        val s = (rem % 60_000) / 1000

                        Text(
                            text = if (h > 0) "%02d:%02d:%02d".format(h, m, s)
                            else "%02d:%02d".format(m, s),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            letterSpacing = (-2).sp
                        )
                        Text(
                            text = "ESTIMATED COMPLETION",
                            fontSize = 10.sp, letterSpacing = 3.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // DATA GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SessionStatCard(
                        modifier = Modifier.weight(1f),
                        label = "By-pass",
                        value = "${session.distractionAttempts}",
                        color = NeonRed,
                        icon = "🚫"
                    )
                    SessionStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Duration",
                        value = "${session.durationMinutes}m",
                        color = NeonAmber,
                        icon = "⏳"
                    )
                }

                // BOTTOM ACTION
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(
                        onClick = viewModel::requestEmergencyUnlock,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "EMERGENCY TERMINATION",
                            color = NeonRed.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }

            } else {
                Text("No active session", color = TextSecondary, fontSize = 16.sp)
            }
        }

        // Dialogs...
        if (uiState.showEmergencyConfirm) {
            EmergencyDialog(
                onDismiss = viewModel::dismissEmergency,
                onConfirm = viewModel::confirmEmergencyIntent
            )
        }

        if (uiState.showMathChallenge) {
            MathChallengeDialog(
                uiState = uiState,
                onSetAnswer = viewModel::setMathAnswer,
                onSubmit = viewModel::submitMathAnswer,
                onCancel = viewModel::dismissEmergency
            )
        }
    }
}

@Composable
fun BreathingRing(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 14.dp.toPx()
        
        // Glow Background
        drawArc(
            color = NeonCyan.copy(alpha = 0.05f * pulse),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth * 2.5f, cap = StrokeCap.Round)
        )
        
        // Track
        drawArc(
            color = SurfaceElevated,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Progress
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(NeonCyan.copy(0.2f), NeonCyan, NeonCyan.copy(0.2f))
            ),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SessionStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    icon: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(text = icon, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                color = TextTertiary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmergencyDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(glowColor = NeonRed) {
            NeonLabel("Critical Override", NeonRed)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Emergency termination will result in a permanent record in your Discipline Engine.",
                color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceElevated)
                ) { Text("Abort", color = TextSecondary) }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) { Text("Terminate", color = Color.White, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
fun MathChallengeDialog(
    uiState: FocusSessionUiState,
    onSetAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        GlassCard(glowColor = NeonAmber) {
            NeonLabel("Validation Protocol", NeonAmber)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Solve the neural challenge:",
                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${uiState.mathA} + ${uiState.mathB} = ?",
                fontSize = 32.sp, fontWeight = FontWeight.Black,
                color = NeonAmber, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = uiState.mathAnswer,
                onValueChange = onSetAnswer,
                placeholder = { Text("Result", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.mathError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonAmber,
                    unfocusedBorderColor = SurfaceElevated,
                    cursorColor = NeonAmber,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                ) { Text("Unlock", color = ObsidianBlack, fontWeight = FontWeight.Black) }
            }
        }
    }
}

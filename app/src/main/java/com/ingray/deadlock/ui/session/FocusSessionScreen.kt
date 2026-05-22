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

    val session = uiState.session

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ring_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(listOf(NeonCyan.copy(0.07f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (session != null) {
                NeonLabel(session.mode.displayName, NeonCyan)

                Text(
                    text = "FOCUS SESSION",
                    fontSize = 11.sp, letterSpacing = 5.sp,
                    color = TextSecondary, fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                // Circular progress ring + timer
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        val progress = session.progressFraction

                        // Track
                        drawArc(
                            color = NeonCyan.copy(alpha = 0.1f),
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false, style = stroke
                        )
                        // Progress
                        drawArc(
                            color = NeonCyan.copy(alpha = ringAlpha),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false, style = stroke
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val rem = uiState.remainingMillis
                        val h = rem / 3_600_000
                        val m = (rem % 3_600_000) / 60_000
                        val s = (rem % 60_000) / 1000

                        Text(
                            text = if (h > 0) "%02d:%02d:%02d".format(h, m, s)
                            else "%02d:%02d".format(m, s),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "REMAINING",
                            fontSize = 9.sp, letterSpacing = 3.sp,
                            color = NeonCyan.copy(alpha = 0.5f)
                        )
                    }
                }

                // Locked apps list
                if (session.lockedPackages.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        NeonLabel("Locked Apps", NeonRed)
                        Spacer(Modifier.height(8.dp))
                        session.lockedPackages.take(8).forEach { pkg ->
                            Text(
                                text = "• ${pkg.substringAfterLast('.')}",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        if (session.lockedPackages.size > 8) {
                            Text(
                                text = "+${session.lockedPackages.size - 8} more",
                                color = TextDisabled, fontSize = 12.sp
                            )
                        }
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${session.distractionAttempts}",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeonRed
                        )
                        Text("BYPASS ATTEMPTS", fontSize = 9.sp, letterSpacing = 2.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${session.durationMinutes}m",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeonAmber
                        )
                        Text("TOTAL DURATION", fontSize = 9.sp, letterSpacing = 2.sp, color = TextSecondary)
                    }
                }

                Spacer(Modifier.weight(1f))

                // Emergency unlock
                TextButton(
                    onClick = viewModel::requestEmergencyUnlock,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Emergency Unlock",
                        color = NeonRed.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }

            } else {
                // No active session
                Text("No active session", color = TextSecondary, fontSize = 16.sp)
            }
        }

        // Emergency confirm dialog
        if (uiState.showEmergencyConfirm) {
            Dialog(onDismissRequest = viewModel::dismissEmergency) {
                GlassCard(glowColor = NeonRed) {
                    NeonLabel("Emergency Override", NeonRed)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Are you sure? This will be recorded and may extend future sessions.",
                        color = TextSecondary, fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::dismissEmergency,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, TextSecondary)
                        ) { Text("Cancel", color = TextSecondary) }
                        Button(
                            onClick = viewModel::confirmEmergencyIntent,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                        ) { Text("Continue", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // Math challenge dialog
        if (uiState.showMathChallenge) {
            Dialog(onDismissRequest = {}) {
                GlassCard(glowColor = NeonAmber) {
                    NeonLabel("Prove Your Intent", NeonAmber)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Solve to unlock:",
                        color = TextSecondary, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${uiState.mathA} + ${uiState.mathB} = ?",
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                        color = NeonAmber, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.mathAnswer,
                        onValueChange = viewModel::setMathAnswer,
                        placeholder = { Text("Answer", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.mathError,
                        supportingText = if (uiState.mathError) {
                            { Text("Wrong answer. Try again.", color = NeonRed) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = NeonAmber,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::dismissEmergency,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, TextSecondary)
                        ) { Text("Cancel", color = TextSecondary) }
                        Button(
                            onClick = viewModel::submitMathAnswer,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                        ) { Text("Unlock", color = BackgroundDeep, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

package com.ingray.deadlock.overlay

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.domain.repository.SettingsRepository
import com.ingray.deadlock.ui.components.GlassCard
import com.ingray.deadlock.ui.theme.DeadLockTheme
import com.ingray.deadlock.ui.theme.NeonAmber
import com.ingray.deadlock.ui.theme.NeonCyan
import com.ingray.deadlock.ui.theme.NeonPurple
import com.ingray.deadlock.ui.theme.NeonRed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class LockOverlayActivity : ComponentActivity() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent this window from being screenshotted and keep screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val appName = getAppName(blockedPackage)

        setContent {
            DeadLockTheme {
                LockOverlayScreen(
                    blockedAppName = appName,
                    blockedPackage = blockedPackage,
                    sessionRepository = sessionRepository,
                    settingsRepository = settingsRepository,
                    onSessionEnded = { finish() }
                )
            }
        }
    }

    // Intercept back — don't allow dismissal
    override fun onBackPressed() {
        // Intentionally blocked: back press is a bypass attempt, do nothing
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}

private fun isTimeInBedtimeRange(startH: Int, startM: Int, endH: Int, endM: Int): Boolean {
    val now = Calendar.getInstance()
    val currentH = now.get(Calendar.HOUR_OF_DAY)
    val currentM = now.get(Calendar.MINUTE)

    val currentTime = currentH * 60 + currentM
    val startTime = startH * 60 + startM
    val endTime = endH * 60 + endM

    return if (startTime <= endTime) {
        currentTime in startTime..endTime
    } else {
        currentTime >= startTime || currentTime <= endTime
    }
}

@Composable
private fun LockOverlayScreen(
    blockedAppName: String,
    blockedPackage: String,
    sessionRepository: SessionRepository,
    settingsRepository: SettingsRepository,
    onSessionEnded: () -> Unit
) {
    BackHandler(enabled = true) { /* block back */ }

    var remainingMillis by remember { mutableLongStateOf(0L) }
    var isBedtimeActive by remember { mutableStateOf(false) }
    var isSessionActive by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()

    // Friction state
    var showFrictionChallenge by remember { mutableStateOf(false) }
    var frictionText by remember { mutableStateOf("") }
    val targetFrictionText = "RCB ".repeat(20).trim()

    LaunchedEffect(Unit) {
        while (isActive) {
            val sessions = sessionRepository.getActiveSessions()
            val settings = settingsRepository.getSettings()
            
            val inBedtime = settings.bedtimeLockEnabled && isTimeInBedtimeRange(
                settings.bedtimeStartHour, settings.bedtimeStartMinute,
                settings.bedtimeEndHour, settings.bedtimeEndMinute
            )
            
            isBedtimeActive = inBedtime

            // Filter for sessions that actually block this specific app
            val relevantSessions = sessions.filter { 
                it.lockedPackages.contains(blockedPackage) || it.mode != com.ingray.deadlock.domain.model.FocusMode.SOFT_FOCUS 
            }

            if (relevantSessions.isEmpty()) {
                isSessionActive = false
                if (!inBedtime) {
                    onSessionEnded()
                    break
                }
            } else {
                isSessionActive = true
                // Show the specific remaining time for this app
                remainingMillis = relevantSessions.maxOf { it.remainingMillis }

                if (remainingMillis <= 0L && !inBedtime) {
                    onSessionEnded()
                    break
                }
            }
            delay(1_000L)
        }
    }

    val hours = remainingMillis / 3_600_000
    val minutes = (remainingMillis % 3_600_000) / 60_000
    val seconds = (remainingMillis % 60_000) / 1_000

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050508)),
        contentAlignment = Alignment.Center
    ) {
        // Background gradient pulse
        Box(
            modifier = Modifier
                .size(400.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonRed.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Lock icon area
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonRed.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 40.sp)
            }

            Text(
                text = if (isBedtimeActive && !isSessionActive) "BEDTIME LOCK" else "DEADLOCK",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = if (isBedtimeActive && !isSessionActive) NeonPurple.copy(alpha = 0.7f) else NeonRed.copy(alpha = 0.7f)
            )

            Text(
                text = blockedAppName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (isBedtimeActive && !isSessionActive) 
                    "is locked to help you rest" 
                    else "is locked during your focus session",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Timer display
            if (isSessionActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F0F18))
                        .padding(horizontal = 40.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (hours > 0)
                                "%02d:%02d:%02d".format(hours, minutes, seconds)
                            else
                                "%02d:%02d".format(minutes, seconds),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "REMAINING",
                            fontSize = 11.sp,
                            letterSpacing = 4.sp,
                            color = NeonCyan.copy(alpha = 0.5f)
                        )
                    }
                }
            } else if (isBedtimeActive) {
                Text(
                    "🌙 Go to sleep. Discipline starts with rest.",
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            val quotes = listOf(
                "Discipline is the bridge between goals and accomplishment.",
                "Focus on the process. The results will follow.",
                "Every minute of resistance makes you stronger.",
                "Your future self is watching. Don't let them down.",
                "The obstacle is the way."
            )
            val quote = remember { quotes.random() }

            Text(
                text = "\"$quote\"",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (!showFrictionChallenge) {
                OutlinedButton(
                    onClick = { showFrictionChallenge = true },
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Emergency Access (2m)", fontSize = 12.sp)
                }
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = NeonAmber
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "SMART FRICTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Type 'RCB' 20 times to unlock",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = frictionText,
                            onValueChange = { 
                                frictionText = it
                                if (it.trim() == targetFrictionText) {
                                    // Bypassed!
                                    scope.launch {
                                        sessionRepository.grantEmergencyAccess(2)
                                        onSessionEnded()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Start typing...", color = Color.White.copy(0.3f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                unfocusedBorderColor = Color.White.copy(0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp)
                        )
                        
                        val progress = (frictionText.length.toFloat() / targetFrictionText.length).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp),
                            color = NeonAmber,
                            trackColor = Color.White.copy(0.1f)
                        )
                        
                        TextButton(onClick = { showFrictionChallenge = false }) {
                            Text("Cancel", color = Color.White.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

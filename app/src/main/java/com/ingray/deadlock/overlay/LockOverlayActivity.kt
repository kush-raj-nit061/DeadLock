package com.ingray.deadlock.overlay

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingray.deadlock.domain.repository.SessionRepository
import com.ingray.deadlock.ui.theme.DeadLockTheme
import com.ingray.deadlock.ui.theme.NeonCyan
import com.ingray.deadlock.ui.theme.NeonRed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

@AndroidEntryPoint
class LockOverlayActivity : ComponentActivity() {

    @Inject
    lateinit var sessionRepository: SessionRepository

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
                    sessionRepository = sessionRepository,
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

@Composable
private fun LockOverlayScreen(
    blockedAppName: String,
    sessionRepository: SessionRepository,
    onSessionEnded: () -> Unit
) {
    BackHandler(enabled = true) { /* block back */ }

    var remainingMillis by remember { mutableLongStateOf(0L) }
    var sessionActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val session = sessionRepository.getActiveSession()
            if (session == null || !session.isActive) {
                sessionActive = false
                onSessionEnded()
                break
            }
            remainingMillis = session.remainingMillis
            if (remainingMillis <= 0L) {
                sessionActive = false
                onSessionEnded()
                break
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
                text = "DEADLOCK",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = NeonRed.copy(alpha = 0.7f)
            )

            Text(
                text = blockedAppName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "is locked during your focus session",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Timer display
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
        }
    }
}

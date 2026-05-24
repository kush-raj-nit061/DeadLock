package com.ingray.deadlock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ingray.deadlock.ui.analytics.AnalyticsScreen
import com.ingray.deadlock.ui.dashboard.DashboardScreen
import com.ingray.deadlock.ui.lock.LockConfigScreen
import com.ingray.deadlock.ui.navigation.Screen
import com.ingray.deadlock.ui.schedule.ScheduleConfigScreen
import com.ingray.deadlock.ui.session.FocusSessionScreen
import com.ingray.deadlock.ui.settings.SettingsScreen
import com.ingray.deadlock.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result ignored, app handles status in UI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestNotifications()
        setContent {
            DeadLockTheme {
                MainScreen()
            }
        }
    }

    private fun checkAndRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onStartFocus = { navController.navigate(Screen.LockConfig.route) },
                    onViewSession = { navController.navigate(Screen.FocusSession.route) }
                )
            }
            composable(Screen.LockConfig.route) {
                LockConfigScreen(
                    onSessionStarted = { navController.navigate(Screen.FocusSession.route) }
                )
            }
            composable(Screen.FocusSession.route) {
                FocusSessionScreen(
                    onSessionEnded = { 
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Schedules.route) {
                ScheduleConfigScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }

        // High-Fidelity Bottom Nav
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            BottomNavBar(navController)
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = listOf(
        Screen.Dashboard to ("Home" to Icons.Outlined.GridView),
        Screen.LockConfig to ("Lock" to Icons.Outlined.Lock),
        Screen.Schedules to ("Auto" to Icons.Outlined.EventRepeat),
        Screen.Analytics to ("Stats" to Icons.Outlined.Insights),
        Screen.Settings to ("Set" to Icons.Outlined.Settings)
    )

    Box(
        modifier = Modifier
            .width(340.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(SurfaceDark.copy(alpha = 0.8f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color.White.copy(0.1f), Color.Transparent)),
                shape = RoundedCornerShape(36.dp)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { (screen, label) ->
                val (_, icon) = label
                val selected = currentDestination?.hierarchy?.any {
                    it.route == screen.route
                } ?: false

                val accentColor = when(screen) {
                    Screen.Dashboard -> NeonCyan
                    Screen.LockConfig -> NeonCyan
                    Screen.Schedules -> NeonCyan
                    Screen.Analytics -> NeonGreen
                    Screen.Settings -> NeonPurple
                    else -> NeonCyan
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                restoreState = true
                                launchSingleTop = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) accentColor else TextTertiary,
                            modifier = Modifier.size(26.dp)
                        )
                        
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(width = 14.dp, height = 3.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        } else {
                            // High-end minimalist: No labels, just icons and active indicators
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }
        }
    }
}

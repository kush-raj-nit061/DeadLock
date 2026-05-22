package com.ingray.deadlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.ingray.deadlock.ui.session.FocusSessionScreen
import com.ingray.deadlock.ui.settings.SettingsScreen
import com.ingray.deadlock.ui.theme.BackgroundDark
import com.ingray.deadlock.ui.theme.DeadLockTheme
import com.ingray.deadlock.ui.theme.NeonAmber
import com.ingray.deadlock.ui.theme.NeonCyan
import com.ingray.deadlock.ui.theme.NeonGreen
import com.ingray.deadlock.ui.theme.NeonPurple
import com.ingray.deadlock.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeadLockTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
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
                    onSessionEnded = { navController.navigate(Screen.Dashboard.route) }
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }

        // Bottom nav
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNavBar(navController)
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = listOf(
        Screen.Dashboard to ("Dashboard" to "📊"),
        Screen.LockConfig to ("Lock" to "🔒"),
        Screen.Analytics to ("Analytics" to "📈"),
        Screen.Settings to ("Settings" to "⚙️")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { (screen, label) ->
                val (title, icon) = label
                val selected = currentDestination?.hierarchy?.any {
                    it.route == screen.route
                } ?: false

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            color = if (selected) NeonCyan.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                restoreState = true
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = icon,
                            fontSize = 18.sp
                        )
                        Text(
                            text = title,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) NeonCyan else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

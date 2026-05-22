package com.ingray.deadlock.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object LockConfig : Screen("lock_config")
    data object FocusSession : Screen("focus_session")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
}

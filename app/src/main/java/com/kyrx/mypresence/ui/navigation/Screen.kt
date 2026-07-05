package com.kyrx.mypresence.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.kyrx.mypresence.ui.components.BottomNavItem

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object GoogleSignIn : Screen("google_sign_in")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Presence : Screen("presence")
}

val bottomNavItems = listOf(
    BottomNavItem(
        icon = Icons.Filled.Home,
        label = "Home",
        route = Screen.Home.route
    ),
    BottomNavItem(
        icon = Icons.Filled.Person,
        label = "Profile",
        route = Screen.Profile.route
    ),
    BottomNavItem(
        icon = Icons.Filled.Settings,
        label = "Settings",
        route = Screen.Settings.route
    )
)

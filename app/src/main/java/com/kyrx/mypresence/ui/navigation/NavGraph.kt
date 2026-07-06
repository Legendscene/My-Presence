package com.kyrx.mypresence.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyrx.mypresence.data.repository.PreferencesRepositoryImpl
import com.kyrx.mypresence.ui.components.PremiumBottomNavBar
import com.kyrx.mypresence.ui.screens.auth.GoogleSignInScreen
import com.kyrx.mypresence.ui.screens.dashboard.DashboardScreen
import com.kyrx.mypresence.ui.screens.onboarding.OnboardingScreen
import com.kyrx.mypresence.ui.screens.profile.ProfileScreen
import com.kyrx.mypresence.ui.screens.settings.SettingsScreen
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.viewmodel.DashboardViewModel
import com.kyrx.mypresence.ui.viewmodel.ProfileViewModel
import com.kyrx.mypresence.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Composable
fun MainApp(
    preferencesRepository: PreferencesRepositoryImpl
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var startDestination by remember { mutableStateOf<String?>(null) }

    // Check if onboarding is completed
    LaunchedEffect(Unit) {
        val onboardingCompleted = preferencesRepository.isOnboardingCompleted.first()
        val accessToken = preferencesRepository.accessToken.first()

        startDestination = when {
            !onboardingCompleted -> Screen.Onboarding.route
            accessToken == null -> Screen.GoogleSignIn.route
            else -> Screen.Home.route
        }
    }

    // Show loading while checking
    if (startDestination == null) {
        Box(modifier = Modifier)
        return
    }

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                PremiumBottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.route,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(
                navController = navController,
                startDestination = startDestination!!,
                preferencesRepository = preferencesRepository
            )
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route,
    preferencesRepository: PreferencesRepositoryImpl
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(300)
            )
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.GoogleSignIn.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                preferencesRepository = preferencesRepository
            )
        }

        composable(Screen.GoogleSignIn.route) {
            GoogleSignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.GoogleSignIn.route) { inclusive = true }
                    }
                },
                onSignInError = { }
            )
        }

        composable(Screen.Home.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(viewModel = viewModel)
        }

        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

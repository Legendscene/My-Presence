package com.kyrx.mypresence.ui.navigation

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.ui.components.AnimatedLogo
import com.kyrx.mypresence.ui.components.PremiumBottomNavBar
import com.kyrx.mypresence.feature.auth.AuthViewModel
import com.kyrx.mypresence.feature.auth.LoginScreen
import com.kyrx.mypresence.feature.dashboard.DashboardScreen
import com.kyrx.mypresence.feature.diagnostics.DiagnosticsScreen
import com.kyrx.mypresence.feature.experimental.ExperimentalEntryScreen
import com.kyrx.mypresence.feature.onboarding.OnboardingScreen
import com.kyrx.mypresence.feature.profile.ProfileScreen
import com.kyrx.mypresence.feature.profile.ProfileViewModel
import com.kyrx.mypresence.feature.settings.SettingsScreen
import com.kyrx.mypresence.feature.settings.SettingsViewModel
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.MyPresenceTheme
import kotlinx.coroutines.flow.first

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Diagnostics : Screen("diagnostics")
    data object Experimental : Screen("experimental")
}

@Composable
fun MainApp(
    preferencesRepository: PreferencesRepository,
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    val isDarkMode by preferencesRepository.isDarkMode.collectAsState(initial = true)

    LaunchedEffect(Unit) {
        val onboardingCompleted = preferencesRepository.isOnboardingCompleted.first()
        startDestination = if (!onboardingCompleted) {
            Screen.Onboarding.route
        } else if (authRepository.authState.value is AuthState.Authenticated) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }

    val authState by authRepository.authState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route, Screen.Profile.route, Screen.Settings.route
    )

    LaunchedEffect(authState) {
        when {
            authState is AuthState.Authenticated && startDestination != null -> {
                val current = navController.currentDestination?.route
                if (current == Screen.Login.route || current == Screen.Onboarding.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            (authState is AuthState.Unauthenticated || authState is AuthState.Error)
                && startDestination != null -> {
                val current = navController.currentDestination?.route
                if (current != Screen.Login.route && current != Screen.Onboarding.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    MyPresenceTheme(darkTheme = isDarkMode) {
        if (startDestination == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Background),
                contentAlignment = Alignment.Center
            ) {
                AnimatedLogo(
                    size = 100.dp,
                    showOrbits = true
                )
            }
            return@MyPresenceTheme
        }

        androidx.compose.material3.Scaffold(
            containerColor = Background,
            bottomBar = {
                if (showBottomBar) {
                    PremiumBottomNavBar(
                        currentRoute = currentRoute ?: Screen.Home.route,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                startDestination = startDestination!!,
                preferencesRepository = preferencesRepository,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    preferencesRepository: PreferencesRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.7f),
                initialOffsetX = { it / 3 }
            ) + fadeIn(animationSpec = spring(dampingRatio = 0.7f))
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = spring(dampingRatio = 0.8f),
                targetOffsetX = { -it / 4 }
            ) + fadeOut(animationSpec = spring(dampingRatio = 0.8f))
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.7f),
                initialOffsetX = { -it / 3 }
            ) + fadeIn(animationSpec = spring(dampingRatio = 0.7f))
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = spring(dampingRatio = 0.8f),
                targetOffsetX = { it / 4 }
            ) + fadeOut(animationSpec = spring(dampingRatio = 0.8f))
        }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                preferencesRepository = preferencesRepository
            )
        }

        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToExperimental = { navController.navigate(Screen.Experimental.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
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

        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Experimental.route) {
            ExperimentalEntryScreen(onBack = { navController.popBackStack() })
        }
    }
}

package com.kyrx.mypresence.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.feature.auth.AuthViewModel
import com.kyrx.mypresence.feature.auth.LoginScreen
import com.kyrx.mypresence.feature.apps.InstalledAppsScreen
import com.kyrx.mypresence.feature.dashboard.DashboardScreen
import com.kyrx.mypresence.feature.diagnostics.DiagnosticsScreen
import com.kyrx.mypresence.feature.presence.PresenceEditorScreen
import com.kyrx.mypresence.feature.presence.CustomPresetsScreen
import com.kyrx.mypresence.feature.presence.CustomPresetsViewModel
import com.kyrx.mypresence.feature.profile.ProfileScreen
import com.kyrx.mypresence.feature.profile.ProfileViewModel
import com.kyrx.mypresence.feature.settings.AboutScreen
import com.kyrx.mypresence.feature.settings.SettingsScreen
import com.kyrx.mypresence.feature.settings.SettingsViewModel
import com.kyrx.mypresence.feature.onboarding.OnboardingScreen
import com.kyrx.mypresence.ui.theme.Accent
import com.kyrx.mypresence.ui.theme.Background
import com.kyrx.mypresence.ui.theme.Dimens
import com.kyrx.mypresence.ui.theme.MyPresenceTheme
import com.kyrx.mypresence.ui.theme.SurfaceElevated
import com.kyrx.mypresence.ui.theme.TextDisabled
import com.kyrx.mypresence.ui.theme.TextPrimary

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val iconFilled: ImageVector) {
    data object Onboarding : Screen("onboarding", "Onboarding", Icons.Outlined.Home, Icons.Filled.Home)
    data object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Profile : Screen("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    data object Login : Screen("login", "Login", Icons.Outlined.Home, Icons.Filled.Home)
    data object Diagnostics : Screen("diagnostics", "Diagnostics", Icons.Outlined.Home, Icons.Filled.Home)
    data object About : Screen("about", "About", Icons.Outlined.Home, Icons.Filled.Home)
    data object PresenceEditor : Screen("presence_editor", "Presence Editor", Icons.Outlined.Home, Icons.Filled.Home)
    data object InstalledApps : Screen("installed_apps", "Apps", Icons.Outlined.Home, Icons.Filled.Home)
    data object CustomPresets : Screen("custom_presets", "Custom Presets", Icons.Outlined.Home, Icons.Filled.Home)
}

val bottomNavScreens = listOf(Screen.Home, Screen.Profile, Screen.Settings)

@Composable
fun MainApp(
    preferencesRepository: PreferencesRepository,
    authRepository: AuthRepository
) {
    val navController = rememberNavController()
    val isDarkMode by preferencesRepository.isDarkMode.collectAsState(initial = true)
    val authState by authRepository.authState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }
    val onboardingCompleted by preferencesRepository.isOnboardingCompleted.collectAsState(initial = false)

    if (authState is AuthState.Loading) {
        MyPresenceTheme(darkTheme = isDarkMode) {
            SplashScreen()
        }
        return
    }

    val startDestination = when {
        !onboardingCompleted -> Screen.Onboarding.route
        authState is AuthState.Authenticated -> Screen.Home.route
        else -> Screen.Login.route
    }

    MyPresenceTheme(darkTheme = isDarkMode) {
        androidx.compose.material3.Scaffold(
            containerColor = Background,
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                        currentRoute = currentRoute ?: Screen.Home.route,
                        onNavigate = { route ->
                            navController.navigate(route, NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setRestoreState(true)
                                .build()
                            )
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                startDestination = startDestination,
                authRepository = authRepository,
                preferencesRepository = preferencesRepository,
                isAuthenticated = authState is AuthState.Authenticated,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    authRepository: AuthRepository,
    preferencesRepository: PreferencesRepository,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = spring(dampingRatio = 0.8f))
        },
        exitTransition = {
            fadeOut(animationSpec = spring(dampingRatio = 0.8f))
        },
        popEnterTransition = {
            fadeIn(animationSpec = spring(dampingRatio = 0.8f))
        },
        popExitTransition = {
            fadeOut(animationSpec = spring(dampingRatio = 0.8f))
        }
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                preferencesRepository = preferencesRepository,
                onComplete = {
                    navController.navigate(
                        if (isAuthenticated) Screen.Home.route
                        else Screen.Login.route
                    ) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                onNavigateToPresenceEditor = { navController.navigate(Screen.PresenceEditor.route) },
                onNavigateToInstalledApps = { navController.navigate(Screen.InstalledApps.route) },
                onNavigateToCustomPresets = { navController.navigate(Screen.CustomPresets.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.PresenceEditor.route) {
            val viewModel: com.kyrx.mypresence.feature.presence.PresenceEditorViewModel = hiltViewModel()
            PresenceEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomPresets.route) {
            val viewModel: CustomPresetsViewModel = hiltViewModel()
            CustomPresetsScreen(
                viewModel = viewModel,
                onNavigateToEditor = {
                    navController.navigate(Screen.PresenceEditor.route) {
                        popUpTo(Screen.CustomPresets.route) { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.InstalledApps.route) {
            InstalledAppsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SplashScreen() {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "splashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.kyrx.mypresence.R.drawable.ic_app_logo),
                contentDescription = "My Presence",
                modifier = Modifier
                    .size(96.dp)
                    .alpha(alpha)
            )
            Spacer(Modifier.height(Dimens.md))
            Text(
                text = "My Presence",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.W600
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavScreens.forEach { screen ->
                val selected = currentRoute == screen.route
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val iconTint by animateColorAsState(
                    targetValue = if (selected) Accent else TextDisabled,
                    animationSpec = tween(300), label = "iconTint"
                )
                val textAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.6f,
                    animationSpec = tween(300), label = "textAlpha"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (selected) Modifier.background(Accent.copy(alpha = 0.1f))
                            else Modifier
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onNavigate(screen.route) }
                        .graphicsLayer {
                            scaleX = if (isPressed) 0.92f else 1f
                            scaleY = if (isPressed) 0.92f else 1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) screen.iconFilled else screen.icon,
                            contentDescription = screen.label,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        if (selected) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = screen.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600,
                                color = TextPrimary.copy(alpha = textAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

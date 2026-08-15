package com.example.solo_levelling.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.ui.achievements.AchievementsScreen
import com.example.solo_levelling.ui.analytics.AnalyticsScreen
import com.example.solo_levelling.ui.character.CharacterScreen
import com.example.solo_levelling.ui.dashboard.DashboardScreen
import com.example.solo_levelling.ui.fitness.FitnessScreen
import com.example.solo_levelling.ui.levelup.LevelUpHost
import com.example.solo_levelling.ui.modules.ModulesScreen
import com.example.solo_levelling.ui.navigation.AppRoute
import com.example.solo_levelling.ui.onboarding.OnboardingScreen
import com.example.solo_levelling.ui.quests.QuestsScreen
import com.example.solo_levelling.ui.settings.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WelcomeMinMs = 3_200L

/** True when bootstrap is ready and the minimum welcome duration has elapsed. */
fun canEnterApp(ready: Boolean, elapsedMs: Long, minMs: Long = WelcomeMinMs): Boolean =
    ready && elapsedMs >= minMs

@Composable
fun SoloLevellingAppRoot(container: AppContainer) {
    val bootstrap: BootstrapViewModel = viewModel(factory = BootstrapViewModel.factory(container))
    val ready by bootstrap.ready.collectAsStateWithLifecycle()
    val onboardingDone by bootstrap.onboardingDone.collectAsStateWithLifecycle()

    var welcomeElapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        delay(WelcomeMinMs)
        welcomeElapsedMs = WelcomeMinMs
    }

    if (!canEnterApp(ready, welcomeElapsedMs)) {
        WelcomeSplash()
        return
    }

    val navController = rememberNavController()
    val start = if (onboardingDone) AppRoute.Dashboard.route else AppRoute.Onboarding.route
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = current in listOf(
        AppRoute.Dashboard.route,
        AppRoute.Quests.route,
        AppRoute.Character.route,
        AppRoute.Fitness.route,
        AppRoute.Modules.route,
        AppRoute.Analytics.route,
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabItems.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route.route,
                            onClick = {
                                navController.navigate(tab.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = start,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(AppRoute.Onboarding.route) {
                    OnboardingScreen(container) {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Onboarding.route) { inclusive = true }
                        }
                    }
                }
                composable(AppRoute.Dashboard.route) {
                    DashboardScreen(
                        container = container,
                        onOpenAchievements = { navController.navigate(AppRoute.Achievements.route) },
                        onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
                        onMessage = showMessage,
                    )
                }
                composable(AppRoute.Quests.route) {
                    QuestsScreen(container = container, onMessage = showMessage)
                }
                composable(AppRoute.Character.route) { CharacterScreen(container) }
                composable(AppRoute.Achievements.route) { AchievementsScreen(container) }
                composable(AppRoute.Fitness.route) { FitnessScreen(container) }
                composable(AppRoute.Modules.route) { ModulesScreen(container) }
                composable(AppRoute.Analytics.route) { AnalyticsScreen(container) }
                composable(AppRoute.Settings.route) {
                    SettingsScreen(container) {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
            LevelUpHost(container)
        }
    }
}

private data class TabItem(val route: AppRoute, val label: String, val icon: ImageVector)

private val tabItems = listOf(
    TabItem(AppRoute.Dashboard, "Home", Icons.Default.Home),
    TabItem(AppRoute.Quests, "Quests", Icons.Default.TaskAlt),
    TabItem(AppRoute.Character, "Self", Icons.Default.Person),
    TabItem(AppRoute.Fitness, "Gym", Icons.Default.FitnessCenter),
    TabItem(AppRoute.Modules, "Life", Icons.Default.Spa),
    TabItem(AppRoute.Analytics, "Review", Icons.Default.Analytics),
)

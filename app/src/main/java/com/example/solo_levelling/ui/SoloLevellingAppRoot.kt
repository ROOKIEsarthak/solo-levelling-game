package com.example.solo_levelling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.ui.achievements.AchievementsScreen
import com.example.solo_levelling.ui.analytics.AnalyticsScreen
import com.example.solo_levelling.ui.career.CareerScreen
import com.example.solo_levelling.ui.character.CharacterScreen
import com.example.solo_levelling.ui.dashboard.DashboardScreen
import com.example.solo_levelling.ui.fitness.FitnessScreen
import com.example.solo_levelling.ui.fitness.FitnessTab
import com.example.solo_levelling.ui.history.HistoryScreen
import com.example.solo_levelling.ui.levelup.LevelUpHost
import com.example.solo_levelling.ui.modules.ModulesScreen
import com.example.solo_levelling.ui.more.MoreScreen
import com.example.solo_levelling.ui.navigation.AppRoute
import com.example.solo_levelling.ui.navigation.buildMainTabs
import com.example.solo_levelling.ui.navigation.redirectForDisabledModuleRoute
import com.example.solo_levelling.ui.navigation.selectedPrimaryRoute
import com.example.solo_levelling.ui.navigation.shouldRestorePrimaryTabState
import com.example.solo_levelling.ui.navigation.showBottomBarForRoute
import com.example.solo_levelling.ui.navigation.sovereignTabLabel
import com.example.solo_levelling.ui.onboarding.OnboardingScreen
import com.example.solo_levelling.ui.quests.QuestsScreen
import com.example.solo_levelling.ui.settings.SettingsScreen
import com.example.solo_levelling.ui.streak.StreakRecoveryHost
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemPrimaryContainer
import com.example.solo_levelling.ui.theme.SystemSurface2
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
    val scope = rememberCoroutineScope()

    val modules by ModuleFlags.observeEnabledModules(
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
        container.db.configDao(),
    ).collectAsStateWithLifecycle(initialValue = EnabledModules())

    val tabItems = remember(modules) { buildTabItems(modules) }
    val showBar = showBottomBarForRoute(current)
    val selectedTab = selectedPrimaryRoute(current)
    val snackbarHostState = remember { SnackbarHostState() }
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val colors = MaterialTheme.colorScheme
    val useRail = LocalConfiguration.current.screenWidthDp >= 840

    val profile by container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .collectAsStateWithLifecycle(initialValue = null)
    val p = profile
    val xpInto = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val xpNeed = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val xpProgress = (xpInto.toFloat() / xpNeed.toFloat()).coerceIn(0f, 1f)

    var fabExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(current, modules) {
        redirectForDisabledModuleRoute(current, modules)?.let { target ->
            navController.navigate(target.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        var lastBest = -1
        container.eventBus.events.collect { event ->
            when (event) {
                is DomainEvent.StreakUpdated -> {
                    SystemMessages.streakMilestone(event.current)?.let { showMessage(it) }
                    if (lastBest >= 0 && event.current > 0 && event.current == event.best && event.best > lastBest) {
                        showMessage(
                            "New personal best\n${event.best} day streak\n" +
                                SystemMessages.pick(SystemMessages.Category.PersonalBest, event.best),
                        )
                    }
                    lastBest = event.best
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (showBar && current == AppRoute.Dashboard.route) {
                Column(horizontalAlignment = Alignment.End) {
                    if (fabExpanded) {
                        FabDialItem("[ ADD TASK ]") {
                            fabExpanded = false
                            navController.navigate(AppRoute.Quests.route)
                        }
                        if (modules.workout) {
                            FabDialItem("[ LOG WORKOUT ]") {
                                fabExpanded = false
                                navController.navigate(AppRoute.Fitness.route)
                            }
                        }
                        if (modules.diet) {
                            FabDialItem("[ ADD MEAL ]") {
                                fabExpanded = false
                                navController.navigate(AppRoute.Nutrition.route)
                            }
                        }
                        FabDialItem("[ ADD WEIGHT ]") {
                            fabExpanded = false
                            navController.navigate(AppRoute.Modules.route)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = SystemPrimaryContainer,
                        contentColor = Color(0xFF05070D),
                    ) {
                        Icon(
                            if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Quick actions",
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (showBar && !useRail) {
                NavigationBar(
                    containerColor = colors.surfaceContainerLow.copy(alpha = 0.95f),
                    contentColor = colors.onSurface,
                ) {
                    tabItems.forEach { tab ->
                        val selected = selectedTab == tab.route.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigatePrimaryTab(navController, tab.route.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.primary.copy(alpha = 0.18f),
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (showBar && useRail) {
                NavigationRail(
                    containerColor = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Text(
                        "◈ SYSTEM",
                        modifier = Modifier.padding(12.dp),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    tabItems.forEach { tab ->
                        val selected = selectedTab == tab.route.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigatePrimaryTab(navController, tab.route.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.primary.copy(alpha = 0.18f),
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                            ),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { navController.navigate(AppRoute.Settings.route) }) {
                        Text(
                            "Settings",
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Column(
                        Modifier
                            .padding(12.dp)
                            .width(72.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "LVL ${p?.level ?: 1}",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        CyberProgressBar(
                            progress = xpProgress,
                            modifier = Modifier.fillMaxWidth(),
                            height = 4.dp,
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
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
                            onOpenWorkout = { navController.navigate(AppRoute.Fitness.route) },
                            onOpenNutrition = { navController.navigate(AppRoute.Nutrition.route) },
                            onOpenMissions = { navController.navigate(AppRoute.Quests.route) },
                            onOpenCareer = { navController.navigate(AppRoute.Career.route) },
                            onOpenCharacter = { navController.navigate(AppRoute.Character.route) },
                            onMessage = showMessage,
                        )
                    }
                    composable(AppRoute.Quests.route) {
                        QuestsScreen(container = container, onMessage = showMessage)
                    }
                    composable(AppRoute.Career.route) {
                        CareerScreen(container = container, onMessage = showMessage)
                    }
                    composable(AppRoute.History.route) {
                        HistoryScreen(
                            container = container,
                            onOpenWorkout = { navController.navigate(AppRoute.Fitness.route) },
                            onOpenDiet = { navController.navigate(AppRoute.Nutrition.route) },
                        )
                    }
                    composable(AppRoute.Character.route) { CharacterScreen(container) }
                    composable(AppRoute.Achievements.route) { AchievementsScreen(container) }
                    composable(AppRoute.Fitness.route) {
                        FitnessScreen(
                            container = container,
                            tab = FitnessTab.Workout,
                            onMessage = showMessage,
                        )
                    }
                    composable(AppRoute.Nutrition.route) {
                        FitnessScreen(
                            container = container,
                            tab = FitnessTab.Diet,
                            onMessage = showMessage,
                        )
                    }
                    composable(AppRoute.Modules.route) {
                        ModulesScreen(container = container, onMessage = showMessage)
                    }
                    composable(AppRoute.Analytics.route) {
                        AnalyticsScreen(container = container, onMessage = showMessage)
                    }
                    composable(AppRoute.More.route) {
                        MoreScreen(
                            onOpenMissions = { navController.navigate(AppRoute.Quests.route) },
                            onOpenProgress = { navController.navigate(AppRoute.Analytics.route) },
                            onOpenSelf = { navController.navigate(AppRoute.Character.route) },
                            onOpenHistory = { navController.navigate(AppRoute.History.route) },
                            onOpenLife = { navController.navigate(AppRoute.Modules.route) },
                            onOpenAchievements = { navController.navigate(AppRoute.Achievements.route) },
                            onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
                            onOpenCareer = { navController.navigate(AppRoute.Career.route) },
                            onOpenWorkout = { navController.navigate(AppRoute.Fitness.route) },
                            onOpenNutrition = { navController.navigate(AppRoute.Nutrition.route) },
                            showCareer = modules.career,
                            showWorkout = modules.workout,
                            showDiet = modules.diet,
                        )
                    }
                    composable(AppRoute.Settings.route) {
                        SettingsScreen(
                            container = container,
                            onMessage = showMessage,
                            onResetComplete = {
                                navController.navigate(AppRoute.Onboarding.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
                LevelUpHost(container)
                StreakRecoveryHost(
                    container = container,
                    onBeginAgain = {
                        navController.navigate(AppRoute.Quests.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

private data class TabItem(val route: AppRoute, val label: String, val icon: ImageVector)

/** Primary tab switch: Home/More never restore a prior child stack. */
private fun navigatePrimaryTab(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = shouldRestorePrimaryTabState(route)
    }
}

private fun buildTabItems(modules: EnabledModules): List<TabItem> =
    buildMainTabs(modules).map { route ->
        val label = sovereignTabLabel(route)
        when (route) {
            AppRoute.Dashboard -> TabItem(route, label, Icons.Default.Home)
            AppRoute.Quests -> TabItem(route, label, Icons.AutoMirrored.Filled.Assignment)
            AppRoute.Analytics -> TabItem(route, label, Icons.Default.Analytics)
            AppRoute.Character -> TabItem(route, label, Icons.Default.Person)
            AppRoute.More -> TabItem(route, label, Icons.Default.MoreHoriz)
            else -> TabItem(route, label, Icons.Default.Home)
        }
    }

@Composable
private fun FabDialItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SystemSurface2.copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        color = SystemPrimary,
        fontFamily = JetBrainsMono,
        style = MaterialTheme.typography.labelMedium,
    )
}

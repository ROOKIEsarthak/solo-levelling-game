package com.example.solo_levelling.ui.navigation

sealed class AppRoute(val route: String) {
    data object Onboarding : AppRoute("onboarding")
    data object Dashboard : AppRoute("dashboard")
    data object Quests : AppRoute("quests")
    data object Character : AppRoute("character")
    data object Achievements : AppRoute("achievements")
    data object Modules : AppRoute("modules")
    data object Fitness : AppRoute("fitness")
    data object Analytics : AppRoute("analytics")
    data object Settings : AppRoute("settings")
}

val mainTabs = listOf(
    AppRoute.Dashboard,
    AppRoute.Quests,
    AppRoute.Character,
    AppRoute.Fitness,
    AppRoute.Modules,
    AppRoute.Analytics,
)

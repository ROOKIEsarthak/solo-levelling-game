package com.example.solo_levelling.ui.navigation

sealed class AppRoute(val route: String) {
    data object SystemConsent : AppRoute("system_consent")
    data object Onboarding : AppRoute("onboarding")
    data object SystemAnalysis : AppRoute("system_analysis")
    data object Dashboard : AppRoute("dashboard")
    data object Career : AppRoute("career")
    data object History : AppRoute("history")
    data object Quests : AppRoute("quests")
    data object Character : AppRoute("character")
    data object Achievements : AppRoute("achievements")
    data object Modules : AppRoute("modules")
    data object Fitness : AppRoute("fitness")
    data object Nutrition : AppRoute("nutrition")
    data object Analytics : AppRoute("analytics")
    data object More : AppRoute("more")
    data object Settings : AppRoute("settings")
}

/** Primary mobile bottom tabs (Sovereign OS shell). */
val mainTabs = listOf(
    AppRoute.Dashboard,
    AppRoute.Quests,
    AppRoute.Analytics,
    AppRoute.Character,
    AppRoute.More,
)

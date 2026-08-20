package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.service.EnabledModules

/**
 * Primary bottom-nav routes for Sovereign OS shell.
 * Career / Fitness / Nutrition are secondary (System Hub), not primary tabs.
 */
fun buildMainTabs(@Suppress("UNUSED_PARAMETER") modules: EnabledModules): List<AppRoute> = listOf(
    AppRoute.Dashboard,
    AppRoute.Quests,
    AppRoute.Analytics,
    AppRoute.Character,
    AppRoute.More,
)

/** Graph path without query strings or Compose argument templates. */
fun routePath(route: String?): String {
    if (route.isNullOrBlank()) return ""
    return route.substringBefore('?').substringBefore("/{")
}

private fun pathMatches(path: String, base: String): Boolean =
    path == base || path.startsWith("$base/")

/** Returns a redirect target when [route] is a module tab that is currently disabled. */
fun redirectForDisabledModuleRoute(route: String?, modules: EnabledModules): AppRoute? {
    val path = routePath(route)
    return when {
        pathMatches(path, AppRoute.Career.route) -> if (!modules.career) AppRoute.Dashboard else null
        pathMatches(path, AppRoute.Fitness.route) -> if (!modules.workout) AppRoute.Dashboard else null
        pathMatches(path, AppRoute.Nutrition.route) -> if (!modules.diet) AppRoute.Dashboard else null
        pathMatches(path, "module_setup") -> null
        else -> null
    }
}

/** Whether the bottom/rail chrome should remain visible for this route. */
fun showBottomBarForRoute(route: String?): Boolean {
    val path = routePath(route)
    return when {
        pathMatches(path, "module_setup") -> false
        path == AppRoute.Dashboard.route -> true
        path == AppRoute.Quests.route -> true
        path == AppRoute.Analytics.route ||
            pathMatches(path, AppRoute.History.route) ||
            pathMatches(path, AppRoute.Achievements.route) -> true
        path == AppRoute.Character.route -> true
        path == AppRoute.More.route ||
            pathMatches(path, AppRoute.Settings.route) ||
            pathMatches(path, AppRoute.Modules.route) ||
            pathMatches(path, AppRoute.Career.route) ||
            pathMatches(path, AppRoute.Fitness.route) ||
            pathMatches(path, AppRoute.Nutrition.route) -> true
        else -> false
    }
}

/** Compact bottom-nav labels — keep short so Material3 bar items do not wrap. */
fun sovereignTabLabel(route: AppRoute): String = when (route) {
    AppRoute.Dashboard -> "HOME"
    AppRoute.Quests -> "QUESTS"
    AppRoute.Analytics -> "PROGRESS"
    AppRoute.Character -> "SELF"
    AppRoute.More -> "MORE"
    else -> route.route.uppercase()
}

/** Max characters for a primary tab label (fits 5-item NavigationBar). */
const val MAX_SOVEREIGN_TAB_LABEL_LENGTH = 8

/** Which primary tab appears selected for a (possibly secondary) route. */
fun selectedPrimaryRoute(route: String?): String {
    val path = routePath(route)
    return when {
        path == AppRoute.Dashboard.route -> AppRoute.Dashboard.route
        path == AppRoute.Quests.route -> AppRoute.Quests.route
        path == AppRoute.Analytics.route ||
            pathMatches(path, AppRoute.History.route) ||
            pathMatches(path, AppRoute.Achievements.route) -> AppRoute.Analytics.route
        path == AppRoute.Character.route -> AppRoute.Character.route
        path == AppRoute.More.route ||
            pathMatches(path, AppRoute.Settings.route) ||
            pathMatches(path, "module_setup") ||
            pathMatches(path, AppRoute.Modules.route) ||
            pathMatches(path, AppRoute.Career.route) ||
            pathMatches(path, AppRoute.Fitness.route) ||
            pathMatches(path, AppRoute.Nutrition.route) -> AppRoute.More.route
        else -> path
    }
}

/**
 * Whether primary-tab navigation should restore a saved back stack for [route].
 * Home, Quests, and More always open the root screen, never a saved child
 * (e.g. Fitness pushed from a quest, Settings under More).
 */
fun shouldRestorePrimaryTabState(route: String): Boolean = when (route) {
    AppRoute.Dashboard.route,
    AppRoute.Quests.route,
    AppRoute.More.route,
    -> false
    else -> true
}

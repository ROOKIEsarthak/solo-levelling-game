package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.service.EnabledModules

/** Primary bottom-nav routes in display order for the given module flags. */
fun buildMainTabs(modules: EnabledModules): List<AppRoute> = buildList {
    add(AppRoute.Dashboard)
    if (modules.career) add(AppRoute.Career)
    if (modules.workout) add(AppRoute.Fitness)
    if (modules.diet) add(AppRoute.Nutrition)
    add(AppRoute.More)
}

/** Returns a redirect target when [route] is a module tab that is currently disabled. */
fun redirectForDisabledModuleRoute(route: String?, modules: EnabledModules): AppRoute? = when (route) {
    AppRoute.Career.route -> if (!modules.career) AppRoute.Dashboard else null
    AppRoute.Fitness.route -> if (!modules.workout) AppRoute.Dashboard else null
    AppRoute.Nutrition.route -> if (!modules.diet) AppRoute.Dashboard else null
    else -> null
}

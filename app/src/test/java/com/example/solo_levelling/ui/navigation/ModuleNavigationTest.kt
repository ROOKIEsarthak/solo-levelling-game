package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.service.EnabledModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModuleNavigationTest {
    @Test
    fun p_allModulesEnabled_includesAllPrimaryTabs() {
        val tabs = buildMainTabs(EnabledModules(career = true, workout = true, diet = true))
        assertEquals(
            listOf(
                AppRoute.Dashboard,
                AppRoute.Career,
                AppRoute.Fitness,
                AppRoute.Nutrition,
                AppRoute.More,
            ),
            tabs,
        )
    }

    @Test
    fun p_careerOnly_showsHomeCareerMore() {
        val tabs = buildMainTabs(EnabledModules(career = true, workout = false, diet = false))
        assertEquals(listOf(AppRoute.Dashboard, AppRoute.Career, AppRoute.More), tabs)
    }

    @Test
    fun n_noModules_showsHomeAndMoreOnly() {
        val tabs = buildMainTabs(EnabledModules())
        assertEquals(listOf(AppRoute.Dashboard, AppRoute.More), tabs)
    }

    @Test
    fun n_disabledCareerRoute_redirectsToDashboard() {
        val redirect = redirectForDisabledModuleRoute(
            AppRoute.Career.route,
            EnabledModules(career = false, workout = true, diet = true),
        )
        assertEquals(AppRoute.Dashboard, redirect)
    }

    @Test
    fun e_enabledWorkoutRoute_noRedirect() {
        val redirect = redirectForDisabledModuleRoute(
            AppRoute.Fitness.route,
            EnabledModules(career = false, workout = true, diet = false),
        )
        assertNull(redirect)
    }

    @Test
    fun e_nonModuleRoute_noRedirect() {
        val redirect = redirectForDisabledModuleRoute(
            AppRoute.Quests.route,
            EnabledModules(),
        )
        assertNull(redirect)
    }
}

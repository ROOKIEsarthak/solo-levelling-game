package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.service.EnabledModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleNavigationTest {
    @Test
    fun p_buildMainTabs_isSovereignFiveTabShell() {
        val tabs = buildMainTabs(EnabledModules(career = true, workout = true, diet = true))
        assertEquals(
            listOf(
                AppRoute.Dashboard,
                AppRoute.Quests,
                AppRoute.Analytics,
                AppRoute.Character,
                AppRoute.More,
            ),
            tabs,
        )
    }

    @Test
    fun e_buildMainTabs_ignoresDisabledModules() {
        val tabs = buildMainTabs(EnabledModules())
        assertEquals(5, tabs.size)
        assertFalse(tabs.contains(AppRoute.Career))
        assertFalse(tabs.contains(AppRoute.Fitness))
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

    @Test
    fun p_selectedPrimaryRoute_fivePrimaryTabs() {
        assertEquals(AppRoute.Dashboard.route, selectedPrimaryRoute(AppRoute.Dashboard.route))
        assertEquals(AppRoute.Quests.route, selectedPrimaryRoute(AppRoute.Quests.route))
        assertEquals(AppRoute.Analytics.route, selectedPrimaryRoute(AppRoute.Analytics.route))
        assertEquals(AppRoute.Character.route, selectedPrimaryRoute(AppRoute.Character.route))
        assertEquals(AppRoute.More.route, selectedPrimaryRoute(AppRoute.More.route))
    }

    @Test
    fun p_selectedPrimaryRoute_progressFamily() {
        assertEquals(AppRoute.Analytics.route, selectedPrimaryRoute(AppRoute.History.route))
        assertEquals(AppRoute.Analytics.route, selectedPrimaryRoute(AppRoute.Achievements.route))
    }

    @Test
    fun p_selectedPrimaryRoute_moreFamily() {
        assertEquals(AppRoute.More.route, selectedPrimaryRoute(AppRoute.Settings.route))
        assertEquals(AppRoute.More.route, selectedPrimaryRoute(AppRoute.Fitness.route))
        assertEquals(AppRoute.More.route, selectedPrimaryRoute(AppRoute.Nutrition.route))
        assertEquals(AppRoute.More.route, selectedPrimaryRoute(AppRoute.Career.route))
        assertEquals(AppRoute.More.route, selectedPrimaryRoute(AppRoute.Modules.route))
    }

    @Test
    fun r_selectedPrimaryRoute_questsIsNotMore() {
        assertEquals(AppRoute.Quests.route, selectedPrimaryRoute(AppRoute.Quests.route))
        assertFalse(selectedPrimaryRoute(AppRoute.Quests.route) == AppRoute.More.route)
    }

    @Test
    fun n_selectedPrimaryRoute_unknownDoesNotDefaultToMore() {
        assertEquals("", selectedPrimaryRoute(null))
        assertEquals("unknown", selectedPrimaryRoute("unknown"))
        assertFalse(selectedPrimaryRoute(null) == AppRoute.More.route)
        assertFalse(selectedPrimaryRoute("unknown") == AppRoute.More.route)
    }

    @Test
    fun n_selectedPrimaryRoute_characterIsNotHome() {
        assertEquals(AppRoute.Character.route, selectedPrimaryRoute(AppRoute.Character.route))
        assertFalse(selectedPrimaryRoute(AppRoute.Character.route) == AppRoute.Dashboard.route)
        assertFalse(selectedPrimaryRoute(AppRoute.Dashboard.route) == AppRoute.Character.route)
    }

    @Test
    fun p_shouldRestorePrimaryTabState_progressAndSelfRestore() {
        assertTrue(shouldRestorePrimaryTabState(AppRoute.Analytics.route))
        assertTrue(shouldRestorePrimaryTabState(AppRoute.Character.route))
    }

    @Test
    fun r_shouldRestorePrimaryTabState_questsNeverRestores() {
        assertFalse(shouldRestorePrimaryTabState(AppRoute.Quests.route))
    }

    @Test
    fun n_shouldRestorePrimaryTabState_homeAndMoreNeverRestore() {
        assertFalse(shouldRestorePrimaryTabState(AppRoute.Dashboard.route))
        assertFalse(shouldRestorePrimaryTabState(AppRoute.More.route))
    }

    @Test
    fun e_shouldRestorePrimaryTabState_unknownRouteRestores() {
        assertTrue(shouldRestorePrimaryTabState(""))
        assertTrue(shouldRestorePrimaryTabState("settings"))
    }

    @Test
    fun p_sovereignTabLabel_characterIsSelf() {
        assertEquals("SELF", sovereignTabLabel(AppRoute.Character))
    }

    @Test
    fun p_sovereignTabLabel_progressIsShort() {
        assertEquals("PROGRESS", sovereignTabLabel(AppRoute.Analytics))
        assertTrue(sovereignTabLabel(AppRoute.Analytics).length <= MAX_SOVEREIGN_TAB_LABEL_LENGTH)
    }

    @Test
    fun e_sovereignTabLabels_fitNavigationBar() {
        buildMainTabs(EnabledModules()).forEach { route ->
            val label = sovereignTabLabel(route)
            assertTrue(
                "Label '$label' for $route exceeds $MAX_SOVEREIGN_TAB_LABEL_LENGTH chars",
                label.length <= MAX_SOVEREIGN_TAB_LABEL_LENGTH,
            )
            assertFalse(label.contains(' '))
        }
    }

    @Test
    fun n_sovereignTabLabel_neverUsesCharacterWord() {
        buildMainTabs(EnabledModules()).forEach { route ->
            assertFalse(
                "Label for $route must not use CHARACTER (wraps on 5-tab bar)",
                sovereignTabLabel(route).contains("CHARACTER"),
            )
        }
    }

    @Test
    fun n_showBottomBar_hidesOnboarding() {
        assertFalse(showBottomBarForRoute(AppRoute.Onboarding.route))
        assertTrue(showBottomBarForRoute(AppRoute.Dashboard.route))
        assertTrue(showBottomBarForRoute(AppRoute.Settings.route))
    }

    @Test
    fun n_showBottomBar_hidesConsentAndAnalysis() {
        assertFalse(showBottomBarForRoute(AppRoute.SystemConsent.route))
        assertFalse(showBottomBarForRoute(AppRoute.SystemAnalysis.route))
    }
}

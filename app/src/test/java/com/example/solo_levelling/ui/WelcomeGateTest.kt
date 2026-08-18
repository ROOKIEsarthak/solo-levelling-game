package com.example.solo_levelling.ui

import com.example.solo_levelling.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeGateTest {
    @Test
    fun p_canEnterApp_whenReadyAndMinDurationMet() {
        assertTrue(canEnterApp(ready = true, elapsedMs = 3_200L, minMs = 3_200L))
    }

    @Test
    fun n_canEnterApp_falseWhenNotReady() {
        assertFalse(canEnterApp(ready = false, elapsedMs = 5_000L, minMs = 3_200L))
    }

    @Test
    fun n_canEnterApp_falseWhenDurationNotMet() {
        assertFalse(canEnterApp(ready = true, elapsedMs = 3_199L, minMs = 3_200L))
    }

    @Test
    fun e_canEnterApp_falseAtZeroElapsedEvenIfReady() {
        assertFalse(canEnterApp(ready = true, elapsedMs = 0L, minMs = 3_200L))
    }

    @Test
    fun e_canEnterApp_trueWhenElapsedExceedsMin() {
        assertTrue(canEnterApp(ready = true, elapsedMs = 4_000L, minMs = 3_200L))
    }

    @Test
    fun p_canEnterApp_defaultMinMsIs3200() {
        assertFalse(canEnterApp(ready = true, elapsedMs = 3_199L))
        assertTrue(canEnterApp(ready = true, elapsedMs = 3_200L))
    }

    @Test
    fun p_startRoute_newUserGoesToConsent() {
        assertEquals(AppRoute.SystemConsent.route, startRoute(onboardingDone = false))
    }

    @Test
    fun p_startRoute_existingUserGoesToDashboard() {
        assertEquals(AppRoute.Dashboard.route, startRoute(onboardingDone = true))
    }

    @Test
    fun n_startRoute_doesNotStartOnboardingOrAnalysis() {
        assertEquals(AppRoute.SystemConsent.route, startRoute(false))
        assertTrue(startRoute(false) != AppRoute.Onboarding.route)
        assertTrue(startRoute(false) != AppRoute.SystemAnalysis.route)
        assertTrue(startRoute(true) != AppRoute.SystemConsent.route)
    }

    @Test
    fun p_lockStartRoute_returningUserStartsOnDashboard() {
        assertEquals(
            AppRoute.Dashboard.route,
            lockStartRoute(locked = null, onboardingDone = true),
        )
    }

    @Test
    fun r_lockStartRoute_keepsConsentWhenOnboardingCompletesMidSession() {
        val locked = lockStartRoute(locked = null, onboardingDone = false)
        assertEquals(AppRoute.SystemConsent.route, locked)
        assertEquals(
            AppRoute.SystemConsent.route,
            lockStartRoute(locked = locked, onboardingDone = true),
        )
    }

    @Test
    fun n_lockStartRoute_doesNotJumpToDashboardWhileLockedOnConsent() {
        val locked = AppRoute.SystemConsent.route
        assertEquals(
            AppRoute.SystemConsent.route,
            lockStartRoute(locked = locked, onboardingDone = true),
        )
        assertEquals(
            AppRoute.Dashboard.route,
            startRoute(onboardingDone = true),
        )
    }
}

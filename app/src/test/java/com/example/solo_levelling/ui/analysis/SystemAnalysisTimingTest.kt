package com.example.solo_levelling.ui.analysis

import com.example.solo_levelling.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemAnalysisTimingTest {
    @Test
    fun p_analysisPhaseAt_staysLoadingUntilFiveSeconds() {
        assertEquals(AnalysisPhase.Loading, analysisPhaseAt(0))
        assertEquals(AnalysisPhase.Loading, analysisPhaseAt(2_500))
        assertEquals(AnalysisPhase.Loading, analysisPhaseAt(4_999))
        assertEquals(AnalysisPhase.Ready, analysisPhaseAt(5_000))
        assertEquals(AnalysisPhase.Ready, analysisPhaseAt(8_000))
    }

    @Test
    fun e_analysisPhaseAt_respectsCustomMinMs() {
        assertEquals(AnalysisPhase.Loading, analysisPhaseAt(999, minMs = 1_000))
        assertEquals(AnalysisPhase.Ready, analysisPhaseAt(1_000, minMs = 1_000))
    }

    @Test
    fun p_canLeaveAnalysis_trueWhenWorkDoneAndMinElapsed() {
        assertTrue(canLeaveAnalysis(elapsedMs = 5_000, workDone = true))
        assertTrue(canLeaveAnalysis(elapsedMs = 6_500, workDone = true))
    }

    @Test
    fun n_canLeaveAnalysis_falseBeforeMinDurationEvenIfWorkDone() {
        assertFalse(canLeaveAnalysis(elapsedMs = 4_999, workDone = true))
        assertFalse(canLeaveAnalysis(elapsedMs = 0, workDone = true))
    }

    @Test
    fun n_canLeaveAnalysis_falseIfWorkNotDoneEvenAfterMin() {
        assertFalse(canLeaveAnalysis(elapsedMs = 5_000, workDone = false))
        assertFalse(canLeaveAnalysis(elapsedMs = 8_000, workDone = false))
    }

    @Test
    fun e_analysisProgress_clampsAtZeroAndOne() {
        assertEquals(0f, analysisProgress(0), 0.0001f)
        assertEquals(0.5f, analysisProgress(2_500), 0.0001f)
        assertEquals(1f, analysisProgress(5_000), 0.0001f)
        assertEquals(1f, analysisProgress(9_000), 0.0001f)
    }

    @Test
    fun p_analysisCopy_isSingleLoadingScreen() {
        assertEquals("SYSTEM INITIALIZING", analysisHeading(AnalysisPhase.Loading))
        assertEquals("Preparing your path...", analysisStatus(AnalysisPhase.Loading))
        assertEquals("SYSTEM READY", analysisHeading(AnalysisPhase.Ready))
        assertEquals("Your path is ready.", analysisStatus(AnalysisPhase.Ready))
        val joined = AnalysisPhase.entries.joinToString(" ") {
            analysisHeading(it) + " " + analysisStatus(it)
        }.lowercase()
        assertFalse(joined.contains("ai"))
        assertFalse(joined.contains("personality"))
        assertFalse(joined.contains("medical"))
        assertFalse(joined.contains("psychological"))
        assertFalse(joined.contains("understanding your profile"))
        assertFalse(joined.contains("building your starting point"))
    }

    @Test
    fun p_analysisRedirectRoute_staysWhenInputPresent() {
        assertNull(analysisRedirectRoute(hasInput = true, onboardingDone = false))
        assertNull(analysisRedirectRoute(hasInput = true, onboardingDone = true))
    }

    @Test
    fun n_analysisRedirectRoute_consentWhenMissingInputAndNotOnboarded() {
        assertEquals(
            AppRoute.SystemConsent.route,
            analysisRedirectRoute(hasInput = false, onboardingDone = false),
        )
    }

    @Test
    fun e_analysisRedirectRoute_dashboardWhenAlreadyOnboarded() {
        assertEquals(
            AppRoute.Dashboard.route,
            analysisRedirectRoute(hasInput = false, onboardingDone = true),
        )
    }

    @Test
    fun r_canLeaveAnalysis_fastWorkCannotSkipFiveSecondDwell() {
        assertFalse(canLeaveAnalysis(elapsedMs = 1_000, workDone = true))
        assertFalse(canLeaveAnalysis(elapsedMs = 3_000, workDone = true))
        assertFalse(canLeaveAnalysis(elapsedMs = 4_999, workDone = true))
        assertTrue(canLeaveAnalysis(elapsedMs = 5_000, workDone = true))
    }
}

package com.example.solo_levelling.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUiHelpersTest {

    @Test
    fun p_onboardingStepTitle_mapsKnownSteps() {
        assertEquals("Establish Identity", onboardingStepTitle(OnboardingStep.NAME))
        assertEquals("System Summary", onboardingStepTitle(OnboardingStep.SUMMARY))
    }

    @Test
    fun p_onboardingProgressFraction_firstStep() {
        assertEquals(0.25f, onboardingProgressFraction(0, 4), 0.001f)
    }

    @Test
    fun p_onboardingProgressFraction_lastStep() {
        assertEquals(1f, onboardingProgressFraction(3, 4), 0.001f)
    }

    @Test
    fun e_onboardingProgressFraction_zeroSteps() {
        assertEquals(0f, onboardingProgressFraction(0, 0), 0.001f)
    }

    @Test
    fun n_onboardingProgressFraction_neverExceedsOne() {
        assertTrue(onboardingProgressFraction(99, 4) <= 1f)
    }
}

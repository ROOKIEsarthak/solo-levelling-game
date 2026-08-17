package com.example.solo_levelling.ui.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsScreenTest {
    @Test
    fun p_formatCompletionRate() {
        assertEquals("80%", formatCompletionRate(0.8f))
    }

    @Test
    fun e_formatCompletionRate_zero() {
        assertEquals("0%", formatCompletionRate(0f))
    }

    @Test
    fun p_improvementCopy() {
        assertTrue(improvementCopy(12.5f).contains("12.5"))
    }

    @Test
    fun p_nextFocusCopy() {
        assertEquals("Focus — an area to invest in.", nextFocusCopy("FOC"))
    }

    @Test
    fun n_nextFocusCopy_unknownCode() {
        assertTrue(nextFocusCopy("XYZ").contains("XYZ"))
    }

    @Test
    fun p_weeklyReviewEncouragement() {
        assertTrue(weeklyReviewEncouragement(42).isNotBlank())
    }

    @Test
    fun e_weeklyReviewEncouragement_stableForSeed() {
        assertEquals(weeklyReviewEncouragement(7), weeklyReviewEncouragement(7))
    }
}

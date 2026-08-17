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
        assertTrue(nextFocusCopy("FOC").contains("FOC"))
    }
}

package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveServicePureTest {
    @Test
    fun p_suggestedXp_increasesWhenCompletionHigh() {
        assertEquals(115, AdaptiveService.suggestedXp(100, 0.95f))
    }

    @Test
    fun p_suggestedXp_decreasesWhenCompletionLow() {
        assertEquals(85, AdaptiveService.suggestedXp(100, 0.2f))
    }

    @Test
    fun e_suggestedXp_unchangedInMiddleBand() {
        assertEquals(100, AdaptiveService.suggestedXp(100, 0.7f))
    }
}

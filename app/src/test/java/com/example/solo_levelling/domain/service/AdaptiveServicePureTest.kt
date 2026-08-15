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

    @Test
    fun e_suggestedXp_boundaryAtNinetyPercent() {
        assertEquals(115, AdaptiveService.suggestedXp(100, 0.9f))
        assertEquals(100, AdaptiveService.suggestedXp(100, 0.89f))
    }

    @Test
    fun e_suggestedXp_boundaryAtFortyPercent() {
        assertEquals(85, AdaptiveService.suggestedXp(100, 0.4f))
        assertEquals(100, AdaptiveService.suggestedXp(100, 0.41f))
    }

    @Test
    fun p_filterDismissed_keepsActiveSuggestions() {
        val suggestions = listOf(
            AdaptiveSuggestion("boost_INT", "Boost INT", "hint"),
            AdaptiveSuggestion("harder_quests", "Ready", "hint"),
        )
        val filtered = AdaptiveService.filterDismissed(suggestions, setOf("harder_quests"))
        assertEquals(1, filtered.size)
        assertEquals("boost_INT", filtered.first().key)
    }

    @Test
    fun n_filterDismissed_emptyWhenAllDismissed() {
        val suggestions = listOf(AdaptiveSuggestion("boost_STR", "Boost STR", "hint"))
        assertEquals(0, AdaptiveService.filterDismissed(suggestions, setOf("boost_STR")).size)
    }

    @Test
    fun n_filterDismissed_emptyInputReturnsEmpty() {
        assertEquals(0, AdaptiveService.filterDismissed(emptyList(), setOf("boost_INT")).size)
    }

    @Test
    fun p_filterDismissed_noDismissedKeysReturnsAll() {
        val suggestions = listOf(
            AdaptiveSuggestion("boost_INT", "Boost INT", "hint"),
            AdaptiveSuggestion("boost_STR", "Boost STR", "hint"),
        )
        assertEquals(2, AdaptiveService.filterDismissed(suggestions, emptySet()).size)
    }
}

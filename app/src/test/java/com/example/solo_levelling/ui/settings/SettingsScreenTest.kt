package com.example.solo_levelling.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenTest {

    @Test
    fun p_isWipeConfirmationValid_exactPhrase() {
        assertTrue(isWipeConfirmationValid("CONFIRM_WIPE"))
    }

    @Test
    fun p_isWipeConfirmationValid_trimsWhitespace() {
        assertTrue(isWipeConfirmationValid("  CONFIRM_WIPE  "))
    }

    @Test
    fun n_isWipeConfirmationValid_wrongPhrase() {
        assertFalse(isWipeConfirmationValid("confirm_wipe"))
    }

    @Test
    fun n_isWipeConfirmationValid_empty() {
        assertFalse(isWipeConfirmationValid(""))
    }

    @Test
    fun e_isWipeConfirmationValid_partialMatch() {
        assertFalse(isWipeConfirmationValid("CONFIRM"))
    }

    @Test
    fun p_systemWipeDescription_mentionsOnboardingRerun() {
        val text = systemWipeDescription()
        assertTrue(text.contains("Onboarding will run again"))
        assertTrue(text.contains("Preserves your name and configs"))
    }

    @Test
    fun n_systemWipeDescription_doesNotPreserveOnboardingFlag() {
        assertFalse(systemWipeDescription().contains("onboarding flag"))
    }

    @Test
    fun e_systemWipeDescription_stillMentionsClearedProgress() {
        val text = systemWipeDescription()
        assertTrue(text.contains("Clears XP"))
        assertTrue(text.contains("quests"))
    }

    @Test
    fun p_settingsSplitIsLocked_whenIdSaved() {
        assertTrue(settingsSplitIsLocked("ppl_ul"))
    }

    @Test
    fun n_settingsSplitIsLocked_blankIsUnlocked() {
        assertFalse(settingsSplitIsLocked(null))
        assertFalse(settingsSplitIsLocked(""))
        assertFalse(settingsSplitIsLocked("   "))
    }

    @Test
    fun p_settingsCurrentSplitLines_usesWeekdayLabels() {
        val lines = settingsCurrentSplitLines("ppl_ul", mapOf(1 to 2, 2 to 3, 3 to 4, 4 to 6, 5 to 7))
        assertTrue(lines.any { it.contains("Tue") })
        assertTrue(lines.any { it.contains("Wed") })
        assertEquals(5, lines.size)
    }

    @Test
    fun e_settingsCurrentSplitLines_unknownSplitFallsBackToId() {
        assertEquals(listOf("missing_split"), settingsCurrentSplitLines("missing_split", emptyMap()))
    }

    @Test
    fun n_lastActiveModuleMessage_explainsConstraint() {
        assertEquals("At least one module must remain active.", lastActiveModuleMessage())
        assertEquals("Choose another module", lastActiveModuleAction())
    }

    @Test
    fun p_pauseModuleCopy_preservesHistory() {
        val body = pauseModuleBody("Fitness")
        assertTrue(body.contains("history will remain saved"))
        assertTrue(body.contains("quests or progression"))
        assertEquals("Pause Fitness?", pauseModuleTitle("Fitness"))
    }

    @Test
    fun n_settingsCurrentSplitLines_missingDayShowsDash() {
        val lines = settingsCurrentSplitLines("ppl_ul", emptyMap())
        assertTrue(lines.all { it.endsWith("—") })
    }

    @Test
    fun p_moduleChangesSummary_singleAndMultiple() {
        assertEquals("Fitness is now active.", moduleChangesSummary(listOf("workout")))
        assertEquals("Fitness and Career have been added.", moduleChangesSummary(listOf("workout", "career")))
        assertEquals("Your system has been updated.", moduleChangesSummary(emptyList()))
    }

    @Test
    fun p_moduleSetupQueueIntro_countsModules() {
        assertEquals("Before the system can continue, we need a few details.", moduleSetupQueueIntro(1))
        assertEquals("2 new modules need setup.", moduleSetupQueueIntro(2))
    }
}

package com.example.solo_levelling.ui.settings

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
}

package com.example.solo_levelling.ui.streak

import com.example.solo_levelling.domain.copy.SystemMessages
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakRecoveryCopyTest {
    @Test
    fun p_reflectPhase_usesRecoveryContext() {
        val msg = SystemMessages.forContext(SystemMessages.MotivationContext.Recovery, 12)
        assertFalse(msg.isBlank())
        assertFalse(msg.contains("THE STREAK ENDED"))
    }

    @Test
    fun p_continuePhase_usesStreakBrokenContext() {
        val msg = SystemMessages.forContext(SystemMessages.MotivationContext.StreakBroken, 12)
        assertFalse(msg.isBlank())
        assertFalse(msg.uppercase().equals(msg))
    }

    @Test
    fun e_fallQuoteShownOnceAcrossFlow() {
        assertTrue(SystemMessages.FALL_QUESTION.contains("Bruce"))
        assertTrue(SystemMessages.FALL_ANSWER.contains("pick ourselves up"))
        assertTrue(SystemMessages.FALL_ATTRIBUTION.contains("Alfred"))
    }
}

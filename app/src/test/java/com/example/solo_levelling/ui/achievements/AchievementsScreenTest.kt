package com.example.solo_levelling.ui.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsScreenTest {

    @Test
    fun p_achievementsCompletionFraction_typical() {
        assertEquals("1/8", achievementsCompletionFraction(1, 8))
    }

    @Test
    fun n_achievementsCompletionFraction_noneUnlocked() {
        assertEquals("0/18", achievementsCompletionFraction(0, 18))
    }

    @Test
    fun e_achievementsCompletionFraction_allUnlocked() {
        assertEquals("5/5", achievementsCompletionFraction(5, 5))
    }

    @Test
    fun e_achievementsCompletionFraction_zeroTotal() {
        assertEquals("0/0", achievementsCompletionFraction(0, 0))
    }

    @Test
    fun r_achievementsCompletionFraction_isSingleLineReadable() {
        val text = achievementsCompletionFraction(1, 8)
        assertTrue(text.contains('/'))
        assertFalse(text.contains('\n'))
        assertEquals(1, text.count { it == '/' })
    }
}

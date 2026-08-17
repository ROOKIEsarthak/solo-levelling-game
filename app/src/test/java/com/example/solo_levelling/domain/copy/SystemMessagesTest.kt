package com.example.solo_levelling.domain.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMessagesTest {
    @Test
    fun p_missionComplete_includesXp() {
        val msg = SystemMessages.missionComplete(50)
        assertTrue(msg.contains("MISSION COMPLETE"))
        assertTrue(msg.contains("+50 XP"))
    }

    @Test
    fun p_intensityBands_mapImprovement() {
        assertEquals(SystemMessages.Intensity.Baseline, SystemMessages.intensityForImprovement(null))
        assertEquals(SystemMessages.Intensity.Small, SystemMessages.intensityForImprovement(2f))
        assertEquals(SystemMessages.Intensity.Medium, SystemMessages.intensityForImprovement(8f))
        assertEquals(SystemMessages.Intensity.Large, SystemMessages.intensityForImprovement(15f))
        assertEquals(SystemMessages.Intensity.Major, SystemMessages.intensityForImprovement(25f))
        assertEquals(SystemMessages.Intensity.Exceptional, SystemMessages.intensityForImprovement(40f))
    }

    @Test
    fun p_pickLevelUp_deterministicForSeed() {
        val a = SystemMessages.pickLevelUp(SystemMessages.Intensity.Medium, 12)
        val b = SystemMessages.pickLevelUp(SystemMessages.Intensity.Medium, 12)
        assertEquals(a, b)
        assertFalse(a.isBlank())
    }

    @Test
    fun n_pick_emptySafeForUnknownCategoryUsesPool() {
        val msg = SystemMessages.pick(SystemMessages.Category.Recovery, 0)
        assertFalse(msg.isBlank())
    }

    @Test
    fun e_streakMilestones_onlyKnownDays() {
        assertNotNull(SystemMessages.streakMilestone(7))
        assertNotNull(SystemMessages.streakMilestone(100))
        assertNull(SystemMessages.streakMilestone(3))
        assertTrue(SystemMessages.streakMilestone(30)!!.contains("DISCIPLINE"))
    }

    @Test
    fun e_fallQuoteConstants_present() {
        assertTrue(SystemMessages.FALL_QUESTION.contains("Bruce"))
        assertTrue(SystemMessages.FALL_ANSWER.contains("pick ourselves up"))
        assertTrue(SystemMessages.FALL_ATTRIBUTION.contains("Alfred"))
    }
}

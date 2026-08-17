package com.example.solo_levelling.domain.copy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMessagesTest {
    @Test
    fun p_missionComplete_includesXpAndMentorLine() {
        val msg = SystemMessages.missionComplete(50)
        assertTrue(msg.contains("+50 XP"))
        assertTrue(msg.lines().size >= 2)
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
        assertTrue(SystemMessages.streakMilestone(30)!!.contains("30 days"))
    }

    @Test
    fun p_forContext_levelAndRankMilestones() {
        assertFalse(SystemMessages.forContext(SystemMessages.MotivationContext.LevelMilestone, 5).isBlank())
        assertFalse(SystemMessages.forContext(SystemMessages.MotivationContext.RankMilestone, 3).isBlank())
    }

    @Test
    fun p_forContext_recoveryAndStreakBroken() {
        assertFalse(SystemMessages.forContext(SystemMessages.MotivationContext.Recovery, 1).isBlank())
        assertFalse(SystemMessages.forContext(SystemMessages.MotivationContext.StreakBroken, 1).isBlank())
    }

    @Test
    fun e_fallQuoteConstants_present() {
        assertTrue(SystemMessages.FALL_QUESTION.contains("Bruce"))
        assertTrue(SystemMessages.FALL_ANSWER.contains("pick ourselves up"))
        assertTrue(SystemMessages.FALL_ATTRIBUTION.contains("Alfred"))
    }

    @Test
    fun p_forContext_deterministicForSeed() {
        val a = SystemMessages.forContext(SystemMessages.MotivationContext.QuestCompleted, 42)
        val b = SystemMessages.forContext(SystemMessages.MotivationContext.QuestCompleted, 42)
        assertEquals(a, b)
        assertFalse(a.isBlank())
    }

    @Test
    fun e_forContext_negativeSeedUsesAbs() {
        val positive = SystemMessages.forContext(SystemMessages.MotivationContext.DailyStart, 3)
        val negative = SystemMessages.forContext(SystemMessages.MotivationContext.DailyStart, -3)
        assertEquals(positive, negative)
    }

    @Test
    fun p_questCompletedFeedback_startsWithXpAndCalmLine() {
        val msg = SystemMessages.questCompletedFeedback(25, seed = 25)
        assertTrue(msg.startsWith("+25 XP"))
        val calmLine = msg.substringAfter("\n")
        assertFalse(calmLine.isBlank())
        assertFalse(calmLine.uppercase() == calmLine)
    }

    @Test
    fun p_questCompletedFeedback_difficultUsesDifficultPool() {
        val normal = SystemMessages.questCompletedFeedback(10, seed = 0, difficult = false)
        val hard = SystemMessages.questCompletedFeedback(10, seed = 0, difficult = true)
        assertTrue(normal.startsWith("+10 XP"))
        assertTrue(hard.startsWith("+10 XP"))
        assertFalse(normal.substringAfter("\n").isBlank())
        assertFalse(hard.substringAfter("\n").isBlank())
    }

    @Test
    fun p_streakMilestone_knownDays7_14_30() {
        listOf(7, 14, 30).forEach { days ->
            val msg = SystemMessages.streakMilestone(days)
            assertNotNull(msg)
            assertTrue(msg!!.startsWith("$days days."))
            assertFalse(msg.substringAfter("\n").isBlank())
        }
    }

    @Test
    fun n_streakMilestone_nullForNonMilestones() {
        listOf(0, 1, 3, 5, 8, 15, 29, 31, 45).forEach { days ->
            assertNull(SystemMessages.streakMilestone(days))
        }
    }

    @Test
    fun n_softenedPools_noShoutyAllCapsLines() {
        val shoutyPatterns = listOf(
            "DON'T BREAK THE MOMENTUM",
            "SYSTEM IDLE",
            "THE STREAK ENDED",
        )
        SystemMessages.MotivationContext.entries.forEach { context ->
            (0..20).forEach { seed ->
                val msg = SystemMessages.forContext(context, seed)
                shoutyPatterns.forEach { pattern ->
                    assertFalse(
                        "Unexpected shouty copy for $context seed=$seed: $msg",
                        msg.contains(pattern, ignoreCase = true),
                    )
                }
                if (msg.isNotBlank()) {
                    assertFalse(
                        "All-caps line for $context seed=$seed: $msg",
                        msg == msg.uppercase() && msg.any { it.isLetter() },
                    )
                }
            }
        }
    }
}

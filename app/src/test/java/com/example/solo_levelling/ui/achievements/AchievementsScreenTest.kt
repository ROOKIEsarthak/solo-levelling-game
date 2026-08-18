package com.example.solo_levelling.ui.achievements

import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.domain.service.EnabledModules
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

    @Test
    fun r_visibleDefs_excludeDsaWhenCareerOff() {
        val defs = listOf(
            AchievementDefEntity("PROBLEM_SLAYER", "Problem Slayer", "Solve 50", "DSA_SOLVED", 50),
            AchievementDefEntity("FIRST_QUEST", "First Quest", "Complete one", "QUESTS_COMPLETED", 1),
            AchievementDefEntity("SEVEN_DAY", "7 Day", "Streak", "STREAK", 7),
        )
        val visible = visibleAchievementDefs(
            defs,
            EnabledModules(career = false, workout = true, diet = true),
        )
        assertEquals(listOf("FIRST_QUEST", "SEVEN_DAY"), visible.map { it.key })
    }

    @Test
    fun p_visibleDefs_keepDsaWhenCareerOn() {
        val defs = listOf(
            AchievementDefEntity("PROBLEM_SLAYER", "Problem Slayer", "Solve 50", "DSA_SOLVED", 50),
            AchievementDefEntity("FIRST_QUEST", "First Quest", "Complete one", "QUESTS_COMPLETED", 1),
        )
        val visible = visibleAchievementDefs(
            defs,
            EnabledModules(career = true, workout = false, diet = false),
        )
        assertEquals(2, visible.size)
    }

    @Test
    fun n_visibleUnlockedCount_ignoresHiddenCareerUnlocks() {
        val visible = listOf(
            AchievementDefEntity("FIRST_QUEST", "First Quest", "Complete one", "QUESTS_COMPLETED", 1),
        )
        assertEquals(
            1,
            visibleUnlockedCount(setOf("FIRST_QUEST", "PROBLEM_SLAYER"), visible),
        )
    }

    @Test
    fun e_visibleUnlockedCount_emptyWhenNoneMatch() {
        val visible = listOf(
            AchievementDefEntity("FIRST_QUEST", "First Quest", "Complete one", "QUESTS_COMPLETED", 1),
        )
        assertEquals(0, visibleUnlockedCount(setOf("PROBLEM_SLAYER"), visible))
    }
}

package com.example.solo_levelling.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SovereignHelpersTest {
    @Test
    fun p_progressFractionNormal() {
        assertEquals(0.5f, progressFraction(50f, 100f), 0.001f)
    }

    @Test
    fun n_progressFractionZeroTarget() {
        assertEquals(0f, progressFraction(10f, 0f), 0.001f)
    }

    @Test
    fun e_progressFractionClampsAboveOne() {
        assertEquals(1f, progressFraction(200f, 100f), 0.001f)
    }

    @Test
    fun e_progressFractionNegativeCurrent() {
        assertEquals(0f, progressFraction(-5f, 100f), 0.001f)
    }

    @Test
    fun p_bracketizeAddsBrackets() {
        assertEquals("[ START ]", bracketize("START"))
    }

    @Test
    fun n_bracketizePreservesExisting() {
        assertEquals("[ START ]", bracketize("[ START ]"))
    }

    @Test
    fun p_navFamilyPrimaryRoutes() {
        assertEquals(NavFamily.Home, navFamilyForRoute("dashboard"))
        assertEquals(NavFamily.Quests, navFamilyForRoute("quests"))
        assertEquals(NavFamily.Progress, navFamilyForRoute("analytics"))
        assertEquals(NavFamily.Character, navFamilyForRoute("character"))
        assertEquals(NavFamily.More, navFamilyForRoute("more"))
    }

    @Test
    fun e_navFamilySecondaryRoutes() {
        assertEquals(NavFamily.Progress, navFamilyForRoute("history"))
        assertEquals(NavFamily.Progress, navFamilyForRoute("achievements"))
        assertEquals(NavFamily.More, navFamilyForRoute("fitness"))
        assertEquals(NavFamily.More, navFamilyForRoute("settings"))
        assertEquals(NavFamily.None, navFamilyForRoute("onboarding"))
        assertEquals(NavFamily.None, navFamilyForRoute(null))
    }

    @Test
    fun p_primaryRouteForFamily() {
        assertEquals("analytics", primaryRouteForFamily(NavFamily.Progress))
        assertEquals("quests", primaryRouteForFamily(NavFamily.Quests))
    }

    @Test
    fun p_attributeDisplaysRelativeToMax() {
        val displays = attributeDisplays(
            codes = listOf("INT", "FOC", "STR"),
            values = listOf(80, 40, 60),
            lifetimeXp = listOf(100, 20, 50),
        )
        assertEquals(3, displays.size)
        assertEquals(1f, displays[0].fraction, 0.001f)
        assertEquals(0.5f, displays[1].fraction, 0.001f)
        assertEquals(0.75f, displays[2].fraction, 0.001f)
        assertEquals(100, displays[0].lifetimeXp)
    }

    @Test
    fun n_attributeDisplaysEmpty() {
        assertTrue(attributeDisplays(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun e_attributeDisplaysZeroValues() {
        val displays = attributeDisplays(listOf("WIS"), listOf(0))
        assertEquals(0f, displays.single().fraction, 0.001f)
    }

    @Test
    fun p_attributeInsightStrongestAndLowest() {
        val insight = attributeInsight(listOf("INT", "FOC", "STR"), listOf(84, 42, 72))
        assertEquals("INT", insight.strongestCode)
        assertEquals(84, insight.strongestValue)
        assertEquals("FOC", insight.lowestCode)
        assertEquals(42, insight.lowestValue)
    }

    @Test
    fun e_attributeInsightEmpty() {
        val insight = attributeInsight(emptyList(), emptyList())
        assertEquals(null, insight.strongestCode)
        assertEquals(null, insight.lowestCode)
    }

    @Test
    fun p_topAttributeDisplaysLimit() {
        val displays = attributeDisplays(listOf("A", "B", "C", "D"), listOf(10, 40, 30, 20))
        val top = topAttributeDisplays(displays, 2)
        assertEquals(listOf("B", "C"), top.map { it.code })
    }

    @Test
    fun p_formatAttributeRewards() {
        assertEquals("+30 INT  +10 DISC", formatAttributeRewards("""{"INT":30,"DISC":10}"""))
    }

    @Test
    fun n_formatAttributeRewardsBlank() {
        assertEquals("", formatAttributeRewards(""))
        assertEquals("", formatAttributeRewards("{}"))
    }

    @Test
    fun p_questRankForXp() {
        assertEquals("S", questRankForXp(100))
        assertEquals("A", questRankForXp(50))
        assertEquals("B", questRankForXp(25))
        assertEquals("C", questRankForXp(10))
        assertEquals("D", questRankForXp(5))
    }

    @Test
    fun p_greetingForHour() {
        assertEquals("GOOD MORNING", greetingForHour(8))
        assertEquals("GOOD AFTERNOON", greetingForHour(14))
        assertEquals("GOOD EVENING", greetingForHour(19))
        assertEquals("GOOD NIGHT", greetingForHour(2))
    }

    @Test
    fun p_streakSupportCopy() {
        assertTrue(streakSupportCopy(0).contains("one day"))
        assertTrue(streakSupportCopy(3).contains("consistency"))
        assertTrue(streakSupportCopy(12).contains("rhythm"))
    }

    @Test
    fun p_xpProgressLabel() {
        assertEquals("2450 / 3000 XP", xpProgressLabel(2450, 3000))
    }

    @Test
    fun e_xpProgressLabelGuards() {
        assertEquals("0 / 1 XP", xpProgressLabel(-5, 0))
    }
}

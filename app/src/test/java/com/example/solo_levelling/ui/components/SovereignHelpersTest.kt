package com.example.solo_levelling.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("+30 Intelligence  +10 Discipline", formatAttributeRewards("""{"INT":30,"DISC":10}"""))
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
    fun p_humanizeSuggestionTitle_replacesAttributeCodes() {
        assertEquals(
            "Invest in Focus this week",
            humanizeSuggestionTitle("Invest in FOC this week"),
        )
    }

    @Test
    fun p_greetingForHour() {
        assertEquals("Good morning", greetingForHour(8))
        assertEquals("Good afternoon", greetingForHour(14))
        assertEquals("Good evening", greetingForHour(19))
        assertEquals("Welcome back", greetingForHour(2))
    }

    @Test
    fun p_streakSupportCopy() {
        assertTrue(streakSupportCopy(0).contains("one day"))
        assertTrue(streakSupportCopy(3).contains("consistency"))
        assertTrue(streakSupportCopy(12).contains("routine"))
    }

    @Test
    fun p_xpProgressLabel() {
        assertEquals("2450 / 3000 XP", xpProgressLabel(2450, 3000))
    }

    @Test
    fun e_xpProgressLabelGuards() {
        assertEquals("0 / 1 XP", xpProgressLabel(-5, 0))
    }

    @Test
    fun p_attributeDisplayName_allSevenCodes() {
        assertEquals("Strength", attributeDisplayName("STR"))
        assertEquals("Endurance", attributeDisplayName("END"))
        assertEquals("Intelligence", attributeDisplayName("INT"))
        assertEquals("Vitality", attributeDisplayName("VIT"))
        assertEquals("Discipline", attributeDisplayName("DISC"))
        assertEquals("Focus", attributeDisplayName("FOC"))
        assertEquals("Wisdom", attributeDisplayName("WIS"))
    }

    @Test
    fun e_attributePresentation_unknownCodeFallsBack() {
        val presentation = attributePresentation("xyz")
        assertEquals("XYZ", presentation.code)
        assertEquals("XYZ", presentation.displayName)
        assertEquals("Growth", presentation.cues)
        assertTrue(presentation.meaning.isNotBlank())
    }

    @Test
    fun p_attributePresentation_caseInsensitive() {
        assertEquals("Intelligence", attributePresentation("int").displayName)
        assertEquals("Focus", attributePresentation("foc").displayName)
    }

    @Test
    fun p_attributePresentation_cuesAreCompactForCharacterDensity() {
        val str = attributePresentation("STR")
        assertEquals("Physical capability · power · effort", str.cues)
        assertTrue(
            "cues should be shorter than full meaning for compact AttributeRow",
            str.cues.length < str.meaning.length,
        )
        assertFalse(str.cues.contains(','))
    }

    @Test
    fun n_attributePresentation_cuesDistinctFromMeaning() {
        val end = attributePresentation("END")
        assertTrue(end.cues.isNotBlank())
        assertTrue(end.meaning.isNotBlank())
        assertFalse(
            "Character compact path must not reuse the essay meaning as cues",
            end.cues == end.meaning,
        )
    }

    @Test
    fun e_attributePresentation_allKnownCodesHaveSingleLineCues() {
        listOf("STR", "END", "INT", "VIT", "DISC", "FOC", "WIS").forEach { code ->
            val cues = attributePresentation(code).cues
            assertTrue("$code cues blank", cues.isNotBlank())
            assertFalse("$code cues should stay single-line", cues.contains('\n'))
        }
    }

    @Test
    fun p_formatAttributeRewards_singleIntUsesFullName() {
        val formatted = formatAttributeRewards("""{"INT":30}""")
        assertTrue(formatted.contains("+30 Intelligence"))
        assertFalse(formatted.contains("+30 INT"))
    }

    @Test
    fun n_formatAttributeRewards_negativeAmount() {
        assertEquals("-5 Discipline", formatAttributeRewards("""{"DISC":-5}"""))
    }

    @Test
    fun e_formatAttributeRewards_malformedReturnsEmpty() {
        assertEquals("", formatAttributeRewards("not-json"))
    }

    @Test
    fun p_attributeGrowthInsight_usesFullNames() {
        val insight = AttributeInsight(
            strongestCode = "INT",
            strongestValue = 80,
            lowestCode = "FOC",
            lowestValue = 40,
        )
        val copy = attributeGrowthInsight(insight)
        assertTrue(copy.contains("Intelligence"))
        assertTrue(copy.contains("Focus"))
        assertFalse(copy.contains("weakest", ignoreCase = true))
        assertFalse(copy.contains("failing", ignoreCase = true))
        assertFalse(copy.contains("bad", ignoreCase = true))
    }

    @Test
    fun e_attributeGrowthInsight_emptyWhenNoLowest() {
        assertEquals("", attributeGrowthInsight(AttributeInsight(null, 0, null, 0)))
    }

    @Test
    fun e_attributeGrowthInsight_singleAttributeOnly() {
        val insight = AttributeInsight(
            strongestCode = "STR",
            strongestValue = 50,
            lowestCode = "STR",
            lowestValue = 50,
        )
        assertEquals("Strength has more room for investment.", attributeGrowthInsight(insight))
    }

    @Test
    fun p_areaToInvestCopy_fullName() {
        assertEquals("Intelligence — an area to invest in.", areaToInvestCopy("INT"))
    }

    @Test
    fun n_areaToInvestCopy_nullOrBlank() {
        assertEquals("", areaToInvestCopy(null))
        assertEquals("", areaToInvestCopy(""))
        assertEquals("", areaToInvestCopy("   "))
    }


    @Test
    fun p_humanizeNextActionDetail_calmCareerSignal() {
        val detail = humanizeNextActionDetail("DSA is your weakest career signal at 42%.")
        assertFalse(detail.contains("weakest", ignoreCase = true))
        assertTrue(detail.contains("42%"))
        assertTrue(detail.contains("focus next"))
    }

    @Test
    fun p_humanizeNextActionDetail_attributeCodes() {
        val detail = humanizeNextActionDetail("Boost INT today.")
        assertTrue(detail.contains("Intelligence"))
        assertFalse(detail.matches(Regex(".*\\bINT\\b.*")))
    }

    @Test
    fun p_humanizeSuggestionTitle_boostIntUsesFullName() {
        val title = humanizeSuggestionTitle("Boost INT")
        assertTrue(title.contains("Intelligence"))
        assertFalse(title.matches(Regex(".*\\bINT\\b.*")))
    }

    @Test
    fun p_displayLabel_bracketedVsPlain() {
        assertEquals("Today", displayLabel("Today", bracketed = false))
        assertEquals("[ Today ]", displayLabel("Today", bracketed = true))
    }

    @Test
    fun e_displayLabel_trimsWhitespace() {
        assertEquals("Quests", displayLabel("  Quests  "))
    }

    @Test
    fun p_xpToNextLabel_remainingXp() {
        assertEquals("550 XP to the next level", xpToNextLabel(2450, 3000))
    }

    @Test
    fun e_xpToNextLabel_readyWhenRemainingZero() {
        assertEquals("Ready for the next level", xpToNextLabel(3000, 3000))
        assertEquals("Ready for the next level", xpToNextLabel(3500, 3000))
    }
}

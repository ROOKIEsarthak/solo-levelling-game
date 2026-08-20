package com.example.solo_levelling.ui.quests

import org.junit.Assert.assertEquals
import org.junit.Test

class QuestTabDisplayLabelTest {

    @Test
    fun p_displayLabel_mapsAllTabs() {
        assertEquals("Today", QuestTab.TODAY.displayLabel())
        assertEquals("Weekly", QuestTab.WEEKLY.displayLabel())
        assertEquals("Milestones", QuestTab.MILESTONES.displayLabel())
        assertEquals("Bosses", QuestTab.BOSSES.displayLabel())
    }
}

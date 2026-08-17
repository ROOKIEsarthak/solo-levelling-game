package com.example.solo_levelling.ui.quests

import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.ui.components.humanizeSuggestionTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestsScreenCopyTest {
    @Test
    fun p_noQuestsContext_returnsCalmCopy() {
        val msg = SystemMessages.forContext(SystemMessages.MotivationContext.NoQuests, QuestTab.TODAY.ordinal)
        assertFalse(msg.contains("SYSTEM IDLE", ignoreCase = true))
        assertFalse(msg.contains("directive", ignoreCase = true))
        assertTrue(msg.isNotBlank())
    }

    @Test
    fun p_questTitleDisplay_humanizesBoostInt() {
        val title = humanizeSuggestionTitle("Boost INT")
        assertTrue(title.contains("Intelligence"))
        assertFalse(title.matches(Regex(".*\\bINT\\b.*")))
    }

    @Test
    fun p_displayLabel_sentenceCase() {
        assertEquals("Today", QuestTab.TODAY.displayLabel())
        assertEquals("Weekly", QuestTab.WEEKLY.displayLabel())
        assertEquals("Bosses", QuestTab.BOSSES.displayLabel())
    }
}

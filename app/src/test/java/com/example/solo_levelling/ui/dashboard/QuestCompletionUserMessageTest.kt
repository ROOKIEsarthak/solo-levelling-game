package com.example.solo_levelling.ui.dashboard

import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.service.QuestCompletionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestCompletionUserMessageTest {
    @Test
    fun p_completed_usesSystemMessagesFeedback() {
        val result = QuestCompletionService.Result.Completed(
            instanceId = 1L,
            xp = 42,
            newTotalXp = 100,
            newLevel = 2,
            newRank = "E",
        )
        val msg = questCompletionUserMessage(result)
        assertEquals(SystemMessages.questCompletedFeedback(42), msg)
        assertTrue(msg.startsWith("+42 XP"))
        val calmLine = msg.substringAfter("\n")
        assertFalse(calmLine.isBlank())
        assertFalse(calmLine.uppercase() == calmLine)
    }

    @Test
    fun n_alreadyCompleted_calmCopy() {
        assertEquals(
            "Quest already completed",
            questCompletionUserMessage(QuestCompletionService.Result.AlreadyCompleted),
        )
    }

    @Test
    fun n_notFound_calmCopy() {
        assertEquals(
            "Quest not found",
            questCompletionUserMessage(QuestCompletionService.Result.NotFound),
        )
    }

    @Test
    fun n_invalidStatus_calmCopy() {
        assertEquals(
            "Quest can't be completed right now",
            questCompletionUserMessage(QuestCompletionService.Result.InvalidStatus),
        )
    }

    @Test
    fun e_dailyCapReached_calmCopy() {
        assertEquals(
            "Daily XP cap reached",
            questCompletionUserMessage(QuestCompletionService.Result.DailyCapReached),
        )
    }
}

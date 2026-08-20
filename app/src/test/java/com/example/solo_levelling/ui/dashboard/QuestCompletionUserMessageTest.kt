package com.example.solo_levelling.ui.dashboard

import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.service.MilestoneVerificationResult
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
    fun e_moduleDisabled_calmCopy() {
        assertEquals(
            "This quest belongs to a disabled module",
            questCompletionUserMessage(QuestCompletionService.Result.ModuleDisabled),
        )
    }

    @Test
    fun n_requirementsIncomplete_doesNotClaimCompletion() {
        val msg = questCompletionUserMessage(
            QuestCompletionService.Result.RequirementsIncomplete(
                MilestoneVerificationResult(
                    ready = false,
                    completedCount = 1,
                    totalCount = 3,
                    requirements = emptyList(),
                ),
            ),
        )
        assertEquals("Complete remaining requirements first", msg)
        assertFalse(msg.contains("Undo", ignoreCase = true))
    }

    @Test
    fun n_wrongDay_usesSystemDateCopy() {
        assertEquals(
            SystemMessages.DATE_QUEST_WRONG_DAY,
            questCompletionUserMessage(QuestCompletionService.Result.WrongDay),
        )
    }
}

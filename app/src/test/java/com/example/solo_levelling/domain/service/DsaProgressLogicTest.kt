package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DsaProgressLogicTest {
    @Test
    fun p_topicProgress_percentFromSolvedCount() {
        assertEquals(50, DsaProgressLogic.topicProgress(5, 10))
    }

    @Test
    fun p_overallProgress_averagesTopics() {
        assertEquals(75, DsaProgressLogic.overallProgress(listOf(100, 50)))
    }

    @Test
    fun p_problemProgressWeight_statusOrder() {
        assertEquals(1.0f, DsaProgressLogic.problemProgressWeight(DsaProblemStatus.MASTERED))
        assertEquals(0.8f, DsaProgressLogic.problemProgressWeight(DsaProblemStatus.SOLVED))
        assertEquals(0.5f, DsaProgressLogic.problemProgressWeight(DsaProblemStatus.NEEDS_REVIEW))
        assertEquals(0.2f, DsaProgressLogic.problemProgressWeight(DsaProblemStatus.ATTEMPTED))
        assertEquals(0f, DsaProgressLogic.problemProgressWeight(DsaProblemStatus.NOT_STARTED))
    }

    @Test
    fun p_recommendNextTopic_firstIncomplete() {
        val topics = listOf("Arrays" to 100, "Trees" to 80, "Graphs" to 0)
        assertEquals("Trees", DsaProgressLogic.recommendNextTopic(topics))
    }

    @Test
    fun n_topicProgress_zeroTotalReturnsZero() {
        assertEquals(0, DsaProgressLogic.topicProgress(0, 0))
    }

    @Test
    fun e_daysUntilReview_negativeWhenOverdue() {
        val solved = 0L
        val now = 4L * 24L * 60L * 60L * 1000L
        assertEquals(-1L, DsaProgressLogic.daysUntilReview(solved, now, intervalDays = 3))
    }

    @Test
    fun e_reviewDueToday_marksOverdueWhenDue() {
        val snapshot = DsaProgressLogic.reviewDueToday(countDueToday = 2, countDueWeek = 5)
        assertTrue(snapshot.overdue)
        assertEquals(2, snapshot.countDueToday)
    }

    @Test
    fun e_recommendNextTopic_allCompleteReturnsNull() {
        assertNull(DsaProgressLogic.recommendNextTopic(listOf("Arrays" to 100)))
    }
}

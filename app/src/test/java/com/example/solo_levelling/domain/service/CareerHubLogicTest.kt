package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerHubLogicTest {
    @Test
    fun p_dsaOverallProgress_fromTopics() {
        val problems = listOf(
            DsaProblemEntity(id = 1, title = "A", topic = "Arrays", status = "SOLVED"),
            DsaProblemEntity(id = 2, title = "B", topic = "Arrays", status = "NOT_STARTED"),
            DsaProblemEntity(id = 3, title = "C", topic = "Trees", status = "MASTERED"),
        )
        assertEquals(75, CareerHubLogic.dsaOverallProgress(problems))
    }

    @Test
    fun p_careerTrackProgress_averagesNodeStatus() {
        val nodes = listOf(
            CareerNodeEntity(track = "System Design", title = "A", orderIndex = 1, status = "MASTERED"),
            CareerNodeEntity(track = "System Design", title = "B", orderIndex = 2, status = "STARTED"),
        )
        assertEquals(75, CareerHubLogic.careerTrackProgress(nodes, "System Design"))
    }

    @Test
    fun p_lowestMandatoryArea_picksLowestPct() {
        val result = CareerHubLogic.lowestMandatoryArea(
            mandatoryAreas = listOf("DSA", "System Design", "Backend"),
            dsaPct = 80,
            sdPct = 20,
            backendPct = 60,
            behavioralPct = 50,
        )
        assertEquals("System Design" to 20, result)
    }

    @Test
    fun p_recommendNextProblem_prefersCurrentTopic() {
        val problems = listOf(
            DsaProblemEntity(id = 1, title = "Other", topic = "Graphs", status = "NOT_STARTED"),
            DsaProblemEntity(id = 2, title = "Trees 1", topic = "Trees", status = "NOT_STARTED"),
        )
        assertEquals(2L, CareerHubLogic.recommendNextProblem(problems, "Trees")?.id)
    }

    @Test
    fun p_needsReviewCount_solvedPastDue() {
        val now = 10L * 24 * 60 * 60 * 1000
        val solvedAt = now - 4L * 24 * 60 * 60 * 1000
        val problems = listOf(
            DsaProblemEntity(id = 1, title = "Due", status = "SOLVED", solvedAtEpochMs = solvedAt),
            DsaProblemEntity(id = 2, title = "Fresh", status = "SOLVED", solvedAtEpochMs = now),
            DsaProblemEntity(id = 3, title = "Flagged", status = "NEEDS_REVIEW"),
        )
        assertEquals(2, CareerHubLogic.needsReviewCount(problems, now))
    }

    @Test
    fun n_emptyMandatoryAreas_returnsNullPriority() {
        assertNull(
            CareerHubLogic.lowestMandatoryArea(
                mandatoryAreas = emptyList(),
                dsaPct = 10,
                sdPct = 10,
                backendPct = 10,
                behavioralPct = 10,
            ),
        )
    }

    @Test
    fun e_parseCsv_andConfigInt() {
        assertEquals(listOf("DSA", "Backend"), CareerHubLogic.parseCsv(" DSA , Backend "))
        assertEquals(0, CareerHubLogic.configInt(null))
        assertEquals(42, CareerHubLogic.configInt("42"))
        assertEquals(100, CareerHubLogic.configInt("150"))
        assertEquals(0, CareerHubLogic.configInt("bad"))
    }

    @Test
    fun p_sdTopicsProgress_averagesTopicConfidence() {
        val topics = listOf(
            com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity(
                id = "a", title = "A", orderIndex = 1, confidence = 100,
            ),
            com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity(
                id = "b", title = "B", orderIndex = 2, confidence = 50,
            ),
        )
        assertEquals(75, CareerHubLogic.sdTopicsProgress(topics))
    }

    @Test
    fun p_currentSdModule_firstIncompleteTopic() {
        val topics = listOf(
            com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity(
                id = "a", title = "Fundamentals", orderIndex = 1, confidence = 100,
            ),
            com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity(
                id = "b", title = "Scaling", orderIndex = 2, confidence = 40,
            ),
        )
        assertEquals("Scaling", CareerHubLogic.currentSdModule(topics))
    }

    @Test
    fun p_nextConceptStatus_cyclesLearningKnownMastered() {
        assertEquals("KNOWN", CareerHubLogic.nextConceptStatus("LEARNING"))
        assertEquals("MASTERED", CareerHubLogic.nextConceptStatus("KNOWN"))
        assertEquals("LEARNING", CareerHubLogic.nextConceptStatus("MASTERED"))
    }

    @Test
    fun e_sdTopicsProgress_emptyReturnsZero() {
        assertEquals(0, CareerHubLogic.sdTopicsProgress(emptyList()))
        assertNull(CareerHubLogic.currentSdModule(emptyList()))
    }

    @Test
    fun n_nextConceptStatus_unknownDefaultsToLearning() {
        assertEquals("LEARNING", CareerHubLogic.nextConceptStatus("INVALID"))
    }

    @Test
    fun p_confidenceLabel_showsPercentOnly() {
        assertEquals("40% confidence", CareerHubLogic.confidenceLabel(40))
        assertEquals("0% confidence", CareerHubLogic.confidenceLabel(0))
        assertEquals("100% confidence", CareerHubLogic.confidenceLabel(100))
    }

    @Test
    fun e_confidenceLabel_clampsOutOfRange() {
        assertEquals("0% confidence", CareerHubLogic.confidenceLabel(-5))
        assertEquals("100% confidence", CareerHubLogic.confidenceLabel(150))
    }

    @Test
    fun n_confidenceLabel_neverContainsEntityToString() {
        val label = CareerHubLogic.confidenceLabel(19)
        assertTrue(!label.contains("SystemDesignTopicEntity"))
        assertTrue(!label.contains("concepts="))
        assertEquals("19% confidence", label)
    }

    @Test
    fun e_emptyDsa_returnsZeroProgress() {
        assertEquals(0, CareerHubLogic.dsaOverallProgress(emptyList()))
        assertEquals(0, CareerHubLogic.careerTrackProgress(emptyList(), "System Design"))
    }
}

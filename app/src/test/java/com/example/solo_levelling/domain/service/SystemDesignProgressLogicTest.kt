package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemDesignProgressLogicTest {
    @Test
    fun p_topicConfidence_averagesConceptStatuses() {
        val confidence = SystemDesignProgressLogic.topicConfidence(
            listOf(
                SystemDesignConceptStatus.LEARNING,
                SystemDesignConceptStatus.KNOWN,
                SystemDesignConceptStatus.MASTERED,
            ),
        )
        assertEquals(71, confidence)
    }

    @Test
    fun p_overallProgress_averagesTopics() {
        assertEquals(62, SystemDesignProgressLogic.overallProgress(listOf(75, 50)))
    }

    @Test
    fun p_currentModule_firstIncompleteTopic() {
        val topics = listOf("Caching" to 100, "Sharding" to 60, "Messaging" to 0)
        assertEquals("Sharding", SystemDesignProgressLogic.currentModule(topics))
    }

    @Test
    fun n_topicConfidence_emptyReturnsZero() {
        assertEquals(0, SystemDesignProgressLogic.topicConfidence(emptyList()))
    }

    @Test
    fun e_conceptConfidence_knownIs75() {
        assertEquals(75, SystemDesignProgressLogic.conceptConfidence(SystemDesignConceptStatus.KNOWN))
    }

    @Test
    fun e_currentModule_allCompleteReturnsNull() {
        assertNull(SystemDesignProgressLogic.currentModule(listOf("Caching" to 100)))
    }
}

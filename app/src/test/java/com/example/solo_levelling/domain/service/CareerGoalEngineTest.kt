package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerGoalEngineTest {
    @Test
    fun p_studentBand_mapsToSde1Readiness() {
        val result = CareerGoalEngine.assess(
            experienceBand = "Student/Beginner",
            currentRole = "Student",
            targetRole = "SDE1",
            yearsExperience = 0.0,
            dsaConfidence = 20,
            sdConfidence = 10,
        )
        assertEquals("SDE1 Interview Readiness", result.nextGoalTitle)
        assertTrue(result.mandatoryAreas.contains("DSA"))
        assertEquals("Student / Beginner", result.currentLevelLabel)
        assertEquals("SDE1", result.targetLevelLabel)
    }

    @Test
    fun p_sde1ToSde2_includesDsaSdBackendMandatory() {
        val result = CareerGoalEngine.assess(
            experienceBand = "2-3",
            currentRole = "SDE1",
            targetRole = "SDE2",
            yearsExperience = 2.5,
            dsaConfidence = 55,
            sdConfidence = 40,
        )
        assertEquals("SDE2 Interview Readiness", result.nextGoalTitle)
        assertEquals(listOf("DSA", "System Design", "Backend"), result.mandatoryAreas)
        assertTrue(result.recommendedAreas.contains("Behavioral"))
    }

    @Test
    fun p_seniorTarget_emphasizesDesignAndLeadership() {
        val result = CareerGoalEngine.assess(
            experienceBand = "5+",
            currentRole = "SDE2",
            targetRole = "Senior",
            yearsExperience = 6.0,
            dsaConfidence = 80,
            sdConfidence = 65,
        )
        assertEquals("Senior Readiness", result.nextGoalTitle)
        assertTrue(result.mandatoryAreas.contains("System Design"))
        assertTrue(result.mandatoryAreas.contains("Leadership"))
        assertTrue(result.recommendedAreas.contains("Architecture"))
    }

    @Test
    fun n_atOrAboveTarget_returnsMaintenanceGoal() {
        val result = CareerGoalEngine.assess(
            experienceBand = "3-5",
            currentRole = "SDE2",
            targetRole = "SDE1",
            yearsExperience = 4.0,
            dsaConfidence = 30,
            sdConfidence = 70,
        )
        assertEquals("Maintain SDE2 Edge", result.nextGoalTitle)
        assertEquals(listOf("DSA"), result.mandatoryAreas)
    }

    @Test
    fun e_lowDsaOnSde1Path_prioritizesDsaFundamentals() {
        val result = CareerGoalEngine.assess(
            experienceBand = "0-1",
            currentRole = "Intern",
            targetRole = "SDE1",
            yearsExperience = 0.5,
            dsaConfidence = 25,
            sdConfidence = 50,
        )
        assertEquals("SDE1 Interview Readiness", result.nextGoalTitle)
        assertEquals("DSA Fundamentals", result.mandatoryAreas.first())
    }
}

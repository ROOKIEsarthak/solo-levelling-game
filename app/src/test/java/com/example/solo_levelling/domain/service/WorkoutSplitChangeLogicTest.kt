package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSplitChangeLogicTest {
    private val dayMs = 24L * 60 * 60 * 1000

    @Test
    fun p_earlyChange_underSixMonths() {
        val applied = 1_000_000L
        val now = applied + 60 * dayMs
        assertTrue(WorkoutSplitChangeLogic.isEarlyChange(applied, now))
        assertEquals(60L, WorkoutSplitChangeLogic.daysHeld(applied, now))
        assertEquals(8L, WorkoutSplitChangeLogic.weeksHeld(applied, now))
    }

    @Test
    fun p_notEarly_afterSixMonths() {
        val applied = 1_000_000L
        val now = applied + 182 * dayMs
        assertFalse(WorkoutSplitChangeLogic.isEarlyChange(applied, now))
        assertEquals(1f, WorkoutSplitChangeLogic.scaleForNewApply(wasEarly = false), 0.001f)
    }

    @Test
    fun n_nullAppliedAt_notEarly() {
        assertFalse(WorkoutSplitChangeLogic.isEarlyChange(null, 1_000_000L))
        assertEquals(null, WorkoutSplitChangeLogic.daysHeld(null, 1_000_000L))
    }

    @Test
    fun p_earlyScale_isSeventyFivePercent() {
        assertEquals(0.75f, WorkoutSplitChangeLogic.scaleForNewApply(wasEarly = true), 0.001f)
    }

    @Test
    fun e_resolvedScale_clearsAfterHold() {
        val applied = 1_000_000L
        val earlyNow = applied + 30 * dayMs
        assertEquals(
            0.75f,
            WorkoutSplitChangeLogic.resolvedScale(applied, 0.75f, earlyNow),
            0.001f,
        )
        val lateNow = applied + 200 * dayMs
        assertEquals(
            1f,
            WorkoutSplitChangeLogic.resolvedScale(applied, 0.75f, lateNow),
            0.001f,
        )
    }
}

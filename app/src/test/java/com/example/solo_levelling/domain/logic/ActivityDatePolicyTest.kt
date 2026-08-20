package com.example.solo_levelling.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ActivityDatePolicyTest {
    private val today = LocalDate.of(2026, 8, 18)
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)

    @Test
    fun p_relation_todayPastFuture() {
        assertEquals(DayRelation.Today, ActivityDatePolicy.relation(today, today))
        assertEquals(DayRelation.Past, ActivityDatePolicy.relation(today, yesterday))
        assertEquals(DayRelation.Future, ActivityDatePolicy.relation(today, tomorrow))
    }

    @Test
    fun p_canWriteRecord_todayOnly() {
        assertTrue(ActivityDatePolicy.canWriteRecord(today, today))
        assertTrue(ActivityDatePolicy.canAwardProgression(today, today))
    }

    @Test
    fun n_canWriteRecord_rejectsPastAndFuture() {
        assertFalse(ActivityDatePolicy.canWriteRecord(today, yesterday))
        assertFalse(ActivityDatePolicy.canWriteRecord(today, tomorrow))
        assertFalse(ActivityDatePolicy.canAwardProgression(today, yesterday))
        assertFalse(ActivityDatePolicy.canAwardProgression(today, tomorrow))
    }

    @Test
    fun p_canCompleteDailyQuest_sameDayOnly() {
        assertTrue(ActivityDatePolicy.canCompleteDailyQuest(today, today))
    }

    @Test
    fun n_canCompleteDailyQuest_rejectsOtherDays() {
        assertFalse(ActivityDatePolicy.canCompleteDailyQuest(today, yesterday))
        assertFalse(ActivityDatePolicy.canCompleteDailyQuest(today, tomorrow))
    }

    @Test
    fun p_canCompleteWeeklyQuest_duringSameIsoWeek() {
        val sunday = LocalDate.of(2026, 8, 23)
        val tuesday = LocalDate.of(2026, 8, 18)
        val monday = LocalDate.of(2026, 8, 17)
        assertTrue(ActivityDatePolicy.canCompleteWeeklyQuest(tuesday, sunday))
        assertTrue(ActivityDatePolicy.canCompleteWeeklyQuest(monday, sunday))
        assertTrue(ActivityDatePolicy.canCompleteWeeklyQuest(sunday, sunday))
    }

    @Test
    fun n_canCompleteWeeklyQuest_rejectsOtherWeeks() {
        val sunday = LocalDate.of(2026, 8, 23)
        val previousSunday = LocalDate.of(2026, 8, 16)
        val nextMonday = LocalDate.of(2026, 8, 24)
        assertFalse(ActivityDatePolicy.canCompleteWeeklyQuest(previousSunday, sunday))
        assertFalse(ActivityDatePolicy.canCompleteWeeklyQuest(nextMonday, sunday))
    }

    @Test
    fun p_canGoToNextDay_fromPastTowardToday() {
        assertTrue(ActivityDatePolicy.canGoToNextDay(today, yesterday))
        assertTrue(ActivityDatePolicy.canGoToNextDay(today, today.minusDays(3)))
    }

    @Test
    fun n_canGoToNextDay_falseOnTodayAndFuture() {
        assertFalse(ActivityDatePolicy.canGoToNextDay(today, today))
        assertFalse(ActivityDatePolicy.canGoToNextDay(today, tomorrow))
    }

    @Test
    fun e_weekBoundary_mondayIsStartOfWeek() {
        val monday = LocalDate.of(2026, 8, 17)
        val sunday = LocalDate.of(2026, 8, 23)
        assertTrue(ActivityDatePolicy.canCompleteWeeklyQuest(monday, sunday))
        assertFalse(ActivityDatePolicy.canCompleteWeeklyQuest(monday.minusDays(1), sunday))
    }
}

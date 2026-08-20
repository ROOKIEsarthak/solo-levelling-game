package com.example.solo_levelling.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class DayRelation {
    Today,
    Past,
    Future,
}

object ActivityDatePolicy {
    fun relation(today: LocalDate, date: LocalDate): DayRelation = when {
        date.isEqual(today) -> DayRelation.Today
        date.isBefore(today) -> DayRelation.Past
        else -> DayRelation.Future
    }

    fun canWriteRecord(today: LocalDate, date: LocalDate): Boolean =
        relation(today, date) == DayRelation.Today

    fun canAwardProgression(today: LocalDate, date: LocalDate): Boolean =
        canWriteRecord(today, date)

    fun canCompleteDailyQuest(today: LocalDate, scheduledDate: LocalDate): Boolean =
        scheduledDate == today

    fun canCompleteWeeklyQuest(today: LocalDate, scheduledDate: LocalDate): Boolean {
        val weekStart = scheduledDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        return !today.isBefore(weekStart) && !today.isAfter(weekEnd)
    }

    fun canGoToNextDay(today: LocalDate, selected: LocalDate): Boolean =
        selected.isBefore(today)
}

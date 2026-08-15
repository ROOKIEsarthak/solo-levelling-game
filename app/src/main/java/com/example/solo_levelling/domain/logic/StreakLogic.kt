package com.example.solo_levelling.domain.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object StreakLogic {
    fun isStreakBroken(lastCompletedDate: LocalDate?, today: LocalDate): Boolean {
        if (lastCompletedDate == null) return false
        return lastCompletedDate != today && lastCompletedDate != today.minusDays(1)
    }

    fun computeNewStreak(current: Int, lastCompletedDate: LocalDate?, today: LocalDate): Int = when {
        lastCompletedDate == null -> 1
        lastCompletedDate == today -> current
        lastCompletedDate == today.minusDays(1) -> current + 1
        else -> 1
    }

    fun gapDays(lastCompletedDate: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(lastCompletedDate, today)
}

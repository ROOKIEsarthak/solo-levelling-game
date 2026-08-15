package com.example.solo_levelling.domain.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DayBoundaryLogic {
    fun shouldResetStreak(lastCompletedDate: LocalDate?, today: LocalDate, graceDays: Int): Boolean {
        if (lastCompletedDate == null) return false
        val gapDays = ChronoUnit.DAYS.between(lastCompletedDate, today)
        if (gapDays <= 0) return false
        return gapDays > graceDays + 1
    }
}

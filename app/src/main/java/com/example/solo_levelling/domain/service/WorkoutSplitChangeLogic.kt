package com.example.solo_levelling.domain.service

/** Pure rules for six-month split consistency and workout-only early-change scale. */
object WorkoutSplitChangeLogic {
    const val MIN_HOLD_DAYS = 182
    const val EARLY_CHANGE_SCALE = 0.75f
    const val KEY_APPLIED_AT = "workout_split_applied_at_epoch_ms"
    const val KEY_SCALE = "workout_progression_scale"

    private const val MS_PER_DAY = 24L * 60 * 60 * 1000

    fun daysHeld(appliedAtEpochMs: Long?, nowEpochMs: Long): Long? {
        if (appliedAtEpochMs == null || appliedAtEpochMs <= 0L) return null
        return ((nowEpochMs - appliedAtEpochMs).coerceAtLeast(0L) / MS_PER_DAY)
    }

    fun isEarlyChange(appliedAtEpochMs: Long?, nowEpochMs: Long): Boolean {
        val days = daysHeld(appliedAtEpochMs, nowEpochMs) ?: return false
        return days < MIN_HOLD_DAYS
    }

    fun weeksHeld(appliedAtEpochMs: Long?, nowEpochMs: Long): Long =
        (daysHeld(appliedAtEpochMs, nowEpochMs) ?: 0L) / 7L

    fun scaleForNewApply(wasEarly: Boolean): Float =
        if (wasEarly) EARLY_CHANGE_SCALE else 1f

    fun resolvedScale(appliedAtEpochMs: Long?, storedScale: Float?, nowEpochMs: Long): Float {
        if (!isEarlyChange(appliedAtEpochMs, nowEpochMs)) return 1f
        return (storedScale ?: EARLY_CHANGE_SCALE).coerceIn(0.1f, 1f)
    }
}

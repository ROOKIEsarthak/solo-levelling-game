package com.example.solo_levelling.core.config

object SystemDefaults {
    const val PLAYER_ID = 1L
    const val DEFAULT_REQUIRED_MEALS_PER_DAY = 3
    const val DAILY_XP_CAP = 500
    const val QUEST_UNDO_MINUTES = 15
    const val UNDO_XP_PENALTY_PERCENT = 0
    const val UNDO_XP_PENALTY_MIN = 1

    fun undoPenaltyXp(originalAmount: Int, percent: Int = UNDO_XP_PENALTY_PERCENT): Int {
        if (percent <= 0 || originalAmount <= 0) return 0
        return maxOf(UNDO_XP_PENALTY_MIN, originalAmount * percent / 100)
    }
    const val STREAK_GRACE_DAYS = 0
    const val LEVEL_BASE = 100.0
    const val LEVEL_EXPONENT = 1.35

    val RANK_THRESHOLDS: List<Pair<String, Int>> = listOf(
        "E" to 1,
        "D" to 6,
        "C" to 11,
        "B" to 21,
        "A" to 36,
        "S" to 51,
        "SS" to 76,
        "MONARCH" to 100,
    )

    fun xpForNextLevel(level: Int): Int =
        kotlin.math.floor(LEVEL_BASE * Math.pow(level.toDouble(), LEVEL_EXPONENT)).toInt()

    fun rankForLevel(level: Int): String {
        var rank = "E"
        for ((name, minLevel) in RANK_THRESHOLDS) {
            if (level >= minLevel) rank = name else break
        }
        return rank
    }

    /** Total XP required to reach [level] from level 1. */
    fun totalXpForLevel(level: Int): Int {
        if (level <= 1) return 0
        var total = 0
        for (l in 1 until level) {
            total += xpForNextLevel(l)
        }
        return total
    }

    fun levelFromTotalXp(totalXp: Int): Int {
        var level = 1
        var remaining = totalXp
        while (true) {
            val need = xpForNextLevel(level)
            if (remaining < need) return level
            remaining -= need
            level++
            if (level > 500) return level
        }
    }
}

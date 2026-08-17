package com.example.solo_levelling.domain.service

enum class DsaProblemStatus {
    NOT_STARTED,
    ATTEMPTED,
    SOLVED,
    NEEDS_REVIEW,
    MASTERED,
}

data class ReviewDueSnapshot(
    val countDueToday: Int,
    val countDueWeek: Int,
    val overdue: Boolean,
)

object DsaProgressLogic {
    fun topicProgress(solvedOrMastered: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((solvedOrMastered.toDouble() / total) * 100).toInt().coerceIn(0, 100)
    }

    fun overallProgress(topicPercents: List<Int>): Int {
        if (topicPercents.isEmpty()) return 0
        return topicPercents.average().toInt().coerceIn(0, 100)
    }

    fun problemProgressWeight(status: DsaProblemStatus): Float = when (status) {
        DsaProblemStatus.MASTERED -> 1.0f
        DsaProblemStatus.SOLVED -> 0.8f
        DsaProblemStatus.NEEDS_REVIEW -> 0.5f
        DsaProblemStatus.ATTEMPTED -> 0.2f
        DsaProblemStatus.NOT_STARTED -> 0f
    }

    fun reviewDueToday(countDueToday: Int, countDueWeek: Int): ReviewDueSnapshot =
        ReviewDueSnapshot(
            countDueToday = countDueToday.coerceAtLeast(0),
            countDueWeek = countDueWeek.coerceAtLeast(0),
            overdue = countDueToday > 0,
        )

    fun daysUntilReview(
        solvedEpochMs: Long,
        nowEpochMs: Long,
        intervalDays: Int = 3,
    ): Long {
        val intervalMs = intervalDays.toLong() * 24L * 60L * 60L * 1000L
        val dueAt = solvedEpochMs + intervalMs
        val diffMs = dueAt - nowEpochMs
        return diffMs / (24L * 60L * 60L * 1000L)
    }

    fun recommendNextTopic(topics: List<Pair<String, Int>>): String? =
        topics.firstOrNull { it.second < 100 }?.first
}

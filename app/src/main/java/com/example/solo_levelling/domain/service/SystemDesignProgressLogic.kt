package com.example.solo_levelling.domain.service

enum class SystemDesignConceptStatus {
    LEARNING,
    KNOWN,
    MASTERED,
}

object SystemDesignProgressLogic {
    fun conceptConfidence(status: SystemDesignConceptStatus): Int = when (status) {
        SystemDesignConceptStatus.LEARNING -> 40
        SystemDesignConceptStatus.KNOWN -> 75
        SystemDesignConceptStatus.MASTERED -> 100
    }

    fun topicConfidence(conceptStatuses: List<SystemDesignConceptStatus>): Int {
        if (conceptStatuses.isEmpty()) return 0
        val total = conceptStatuses.sumOf { conceptConfidence(it) }
        return (total.toDouble() / conceptStatuses.size).toInt().coerceIn(0, 100)
    }

    fun overallProgress(topicPercents: List<Int>): Int {
        if (topicPercents.isEmpty()) return 0
        return topicPercents.average().toInt().coerceIn(0, 100)
    }

    fun currentModule(topics: List<Pair<String, Int>>): String? =
        topics.firstOrNull { it.second < 100 }?.first
}

package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity

object CareerHubLogic {
    fun parseCsv(csv: String?): List<String> =
        csv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun configInt(value: String?, default: Int = 0): Int =
        value?.toIntOrNull()?.coerceIn(0, 100) ?: default

    fun dsaTopicProgress(problems: List<DsaProblemEntity>): List<Pair<String, Int>> {
        if (problems.isEmpty()) return emptyList()
        return problems.groupBy { it.topic.ifBlank { "General" } }
            .map { (topic, group) ->
                val solved = group.count { it.status == "SOLVED" || it.status == "MASTERED" }
                topic to DsaProgressLogic.topicProgress(solved, group.size)
            }
            .sortedBy { it.first }
    }

    fun dsaOverallProgress(problems: List<DsaProblemEntity>): Int =
        DsaProgressLogic.overallProgress(dsaTopicProgress(problems).map { it.second })

    fun sdTopicsProgress(topics: List<SystemDesignTopicEntity>): Int =
        SystemDesignProgressLogic.overallProgress(topics.map { it.confidence })

    fun currentSdModule(topics: List<SystemDesignTopicEntity>): String? =
        topics.sortedBy { it.orderIndex }
            .firstOrNull { it.confidence < 100 }
            ?.title

    fun nextConceptStatus(current: String): String = when (current.uppercase()) {
        "LEARNING" -> "KNOWN"
        "KNOWN" -> "MASTERED"
        else -> "LEARNING"
    }

    /** Use ${confidence} in UI — never "$topic.confidence" (that dumps the entity toString). */
    fun confidenceLabel(confidence: Int): String =
        "${confidence.coerceIn(0, 100)}% confidence"

    fun careerTrackProgress(nodes: List<CareerNodeEntity>, track: String): Int {
        val trackNodes = nodes.filter { it.track.equals(track, ignoreCase = true) }
        if (trackNodes.isEmpty()) return 0
        val percents = trackNodes.map { nodeProgressPercent(it.status) }
        return percents.average().toInt().coerceIn(0, 100)
    }

    fun nodeProgressPercent(status: String): Int = when (status.uppercase()) {
        "MASTERED" -> 100
        "PRACTICED" -> 75
        "STARTED" -> 50
        else -> 0
    }

    fun areaPercent(
        area: String,
        dsaPct: Int,
        sdPct: Int,
        backendPct: Int,
        behavioralPct: Int,
    ): Int? {
        val key = area.trim().lowercase()
        return when {
            key.contains("dsa") -> dsaPct
            key.contains("system design") || key == "sd" -> sdPct
            key.contains("backend") -> backendPct
            key.contains("behavioral") || key.contains("leadership") -> behavioralPct
            key.contains("architecture") || key.contains("design") -> sdPct
            else -> null
        }
    }

    fun lowestMandatoryArea(
        mandatoryAreas: List<String>,
        dsaPct: Int,
        sdPct: Int,
        backendPct: Int,
        behavioralPct: Int,
    ): Pair<String, Int>? {
        if (mandatoryAreas.isEmpty()) return null
        return mandatoryAreas.mapNotNull { area ->
            val pct = areaPercent(area, dsaPct, sdPct, backendPct, behavioralPct) ?: return@mapNotNull null
            area to pct
        }.minByOrNull { it.second }
    }

    fun currentDsaTopic(problems: List<DsaProblemEntity>): String? =
        DsaProgressLogic.recommendNextTopic(dsaTopicProgress(problems))

    fun recommendNextProblem(
        problems: List<DsaProblemEntity>,
        currentTopic: String?,
    ): DsaProblemEntity? {
        val topic = currentTopic?.ifBlank { null }
        if (topic != null) {
            problems.firstOrNull { it.topic.equals(topic, ignoreCase = true) && it.status == "NOT_STARTED" }
                ?.let { return it }
        }
        return problems.firstOrNull { it.status == "NOT_STARTED" }
    }

    fun needsReviewCount(problems: List<DsaProblemEntity>, nowEpochMs: Long): Int =
        problems.count { problem ->
            when (problem.status) {
                "NEEDS_REVIEW" -> true
                "SOLVED" -> {
                    val solvedAt = problem.solvedAtEpochMs ?: return@count false
                    DsaProgressLogic.daysUntilReview(solvedAt, nowEpochMs) <= 0
                }
                else -> false
            }
        }
}

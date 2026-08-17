package com.example.solo_levelling.domain.service

data class NextAction(
    val title: String,
    val detail: String,
    val reason: String,
    val routeHint: String,
)

object PriorityEngine {
    fun nextAction(
        dsaPct: Int,
        sdPct: Int,
        backendPct: Int,
        behavioralPct: Int,
        mandatoryAreas: List<String>,
        workoutDoneToday: Boolean,
        workoutPlannedToday: Boolean,
        dietCaloriePctOfTarget: Int,
        proteinPctOfTarget: Int,
        openQuestsRemaining: Int,
        modules: EnabledModules = EnabledModules(career = true, workout = true, diet = true),
    ): NextAction {
        if (modules.workout && workoutPlannedToday && !workoutDoneToday) {
            return NextAction(
                title = "Complete today's workout",
                detail = "A planned session is still open.",
                reason = "Fitness block is scheduled and not finished.",
                routeHint = "fitness",
            )
        }

        if (modules.diet && proteinPctOfTarget < 70 && dietCaloriePctOfTarget >= 40) {
            return NextAction(
                title = "Hit protein target",
                detail = "Protein is below 70% of today's goal.",
                reason = "Nutrition is lagging late in the day.",
                routeHint = "nutrition",
            )
        }

        if (modules.career) {
            val mandatoryAction = actionForLowestMandatory(
                mandatoryAreas = mandatoryAreas,
                dsaPct = dsaPct,
                sdPct = sdPct,
                backendPct = backendPct,
                behavioralPct = behavioralPct,
            )
            if (mandatoryAction != null) return mandatoryAction
        }

        if (openQuestsRemaining > 0) {
            return NextAction(
                title = "Finish open quests",
                detail = "$openQuestsRemaining quest(s) still open today.",
                reason = "Daily quests drive consistency and XP.",
                routeHint = "quests",
            )
        }

        if (modules.career) {
            return weakestCareerAction(dsaPct, sdPct, backendPct, behavioralPct)
        }

        return NextAction(
            title = "Keep your streak",
            detail = "No blockers right now — stay consistent.",
            reason = "Enabled modules are clear for today.",
            routeHint = "dashboard",
        )
    }

    private fun actionForLowestMandatory(
        mandatoryAreas: List<String>,
        dsaPct: Int,
        sdPct: Int,
        backendPct: Int,
        behavioralPct: Int,
    ): NextAction? {
        if (mandatoryAreas.isEmpty()) return null

        val candidates = mandatoryAreas.mapNotNull { area ->
            val mapping = areaMapping(area) ?: return@mapNotNull null
            val pct = mapping.pct(dsaPct, sdPct, backendPct, behavioralPct)
            Triple(area, pct, mapping)
        }
        if (candidates.isEmpty()) return null

        val (area, pct, mapping) = candidates.minByOrNull { it.second } ?: return null

        return NextAction(
            title = mapping.title,
            detail = mapping.detail(area, pct),
            reason = "$area is a mandatory focus area at $pct%.",
            routeHint = mapping.routeHint,
        )
    }

    private fun weakestCareerAction(
        dsaPct: Int,
        sdPct: Int,
        backendPct: Int,
        behavioralPct: Int,
    ): NextAction {
        val areas = listOf(
            Triple("DSA", dsaPct, "career_dsa"),
            Triple("System Design", sdPct, "career_sd"),
            Triple("Backend", backendPct, "career_sd"),
            Triple("Behavioral", behavioralPct, "quests"),
        )
        val (name, pct, route) = areas.minByOrNull { it.second }!!
        return NextAction(
            title = "Improve $name",
            detail = "$name is your weakest career signal at $pct%.",
            reason = "No higher-priority blockers; strengthen the lowest area.",
            routeHint = route,
        )
    }

    private data class AreaMapping(
        val matches: (String) -> Boolean,
        val pct: (Int, Int, Int, Int) -> Int,
        val routeHint: String,
        val title: String,
        val detail: (String, Int) -> String,
    )

    private fun areaMapping(area: String): AreaMapping? {
        val key = area.trim().lowercase()
        return when {
            key.contains("dsa") -> AreaMapping(
                matches = { it.contains("dsa") },
                pct = { dsa, _, _, _ -> dsa },
                routeHint = "career_dsa",
                title = "Practice DSA",
                detail = { _, pct -> "Mandatory DSA progress is at $pct%." },
            )
            key.contains("system design") || key == "sd" -> AreaMapping(
                matches = { it.contains("system design") || it == "sd" },
                pct = { _, sd, _, _ -> sd },
                routeHint = "career_sd",
                title = "Study system design",
                detail = { _, pct -> "Mandatory system design progress is at $pct%." },
            )
            key.contains("backend") -> AreaMapping(
                matches = { it.contains("backend") },
                pct = { _, _, backend, _ -> backend },
                routeHint = "career_sd",
                title = "Strengthen backend skills",
                detail = { _, pct -> "Mandatory backend progress is at $pct%." },
            )
            key.contains("behavioral") || key.contains("leadership") -> AreaMapping(
                matches = { it.contains("behavioral") || it.contains("leadership") },
                pct = { _, _, _, behavioral -> behavioral },
                routeHint = "quests",
                title = "Work on behavioral prep",
                detail = { _, pct -> "Mandatory behavioral progress is at $pct%." },
            )
            key.contains("architecture") || key.contains("design") -> AreaMapping(
                matches = { it.contains("architecture") || it.contains("design") },
                pct = { _, sd, _, _ -> sd },
                routeHint = "career_sd",
                title = "Review architecture topics",
                detail = { _, pct -> "Mandatory design progress is at $pct%." },
            )
            else -> null
        }
    }
}

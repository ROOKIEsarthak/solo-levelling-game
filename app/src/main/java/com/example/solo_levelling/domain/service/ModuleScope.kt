package com.example.solo_levelling.domain.service

/**
 * Single boundary for module ownership of XP sources, quests, and achievements.
 * GLOBAL sources (FOCUS, JOURNAL, untagged quests) always participate.
 */
enum class ModuleId {
    CAREER,
    WORKOUT,
    DIET,
    GLOBAL,
}

object ModuleScope {
    fun moduleForSourceType(sourceType: String): ModuleId =
        when (sourceType.uppercase()) {
            "DSA", "DSA_MASTER", "SD_CONCEPT" -> ModuleId.CAREER
            "WORKOUT", "WORKOUT_UNDO" -> ModuleId.WORKOUT
            "NUTRITION", "NUTRITION_UNDO" -> ModuleId.DIET
            "FOCUS", "JOURNAL", "ACHIEVEMENT", "BOSS", "BOSS_UNDO",
            "QUEST_UNDO", "QUEST_UNDO_PENALTY", "SEASON",
            -> ModuleId.GLOBAL
            "QUEST_INSTANCE" -> ModuleId.GLOBAL // refined via quest tags / metadata
            else -> ModuleId.GLOBAL
        }

    fun moduleForPriorityTags(priorityTags: String): ModuleId {
        val tags = priorityTags.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val moduleTags = tags.filter { it.startsWith("module_") }
        if (moduleTags.isEmpty()) return ModuleId.GLOBAL
        // Prefer first known module tag
        for (tag in moduleTags) {
            when (tag) {
                "module_career" -> return ModuleId.CAREER
                "module_workout" -> return ModuleId.WORKOUT
                "module_diet" -> return ModuleId.DIET
            }
        }
        return ModuleId.GLOBAL
    }

    fun moduleForAchievement(criteriaType: String, achievementKey: String = ""): ModuleId =
        when (criteriaType.uppercase()) {
            "DSA_SOLVED" -> ModuleId.CAREER
            "QUESTS_COMPLETED", "STREAK", "LEVEL", "PERFECT_WEEK", "BOSS_CLEARED" -> ModuleId.GLOBAL
            else -> when (achievementKey.uppercase()) {
                "PROBLEM_SLAYER" -> ModuleId.CAREER
                else -> ModuleId.GLOBAL
            }
        }

    fun isEnabled(module: ModuleId, modules: EnabledModules): Boolean =
        when (module) {
            ModuleId.CAREER -> modules.career
            ModuleId.WORKOUT -> modules.workout
            ModuleId.DIET -> modules.diet
            ModuleId.GLOBAL -> true
        }

    fun allowsSourceType(sourceType: String, modules: EnabledModules): Boolean =
        isEnabled(moduleForSourceType(sourceType), modules)

    fun allowsQuestTemplate(priorityTags: String, modules: EnabledModules): Boolean =
        isEnabled(moduleForPriorityTags(priorityTags), modules)

    fun allowsAchievement(criteriaType: String, modules: EnabledModules, achievementKey: String = ""): Boolean =
        isEnabled(moduleForAchievement(criteriaType, achievementKey), modules)

    /**
     * Ledger row allowed under active modules.
     * QUEST_INSTANCE uses metadata "module" when present, otherwise [questModule] from the template.
     */
    fun allowsLedgerEntry(
        sourceType: String,
        metadataJson: String,
        modules: EnabledModules,
        questModule: ModuleId? = null,
    ): Boolean {
        val upper = sourceType.uppercase()
        if (upper == "QUEST_INSTANCE") {
            val fromMeta = parseModuleFromMetadata(metadataJson)
            if (fromMeta != null) return isEnabled(fromMeta, modules)
            if (questModule != null) return isEnabled(questModule, modules)
            return true // untagged / unknown quest XP treated as global
        }
        if (upper == "QUEST_UNDO" || upper == "QUEST_UNDO_PENALTY" ||
            upper == "BOSS_UNDO" || upper == "WORKOUT_UNDO" || upper == "NUTRITION_UNDO"
        ) {
            val fromMeta = parseModuleFromMetadata(metadataJson)
            if (fromMeta != null) return isEnabled(fromMeta, modules)
            return true
        }
        return allowsSourceType(sourceType, modules)
    }

    fun parseModuleFromMetadata(metadataJson: String): ModuleId? {
        val match = Regex(""""module"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
            .find(metadataJson)
            ?.groupValues
            ?.get(1)
            ?.uppercase()
            ?: return null
        return when (match) {
            "CAREER" -> ModuleId.CAREER
            "WORKOUT" -> ModuleId.WORKOUT
            "DIET" -> ModuleId.DIET
            "GLOBAL" -> ModuleId.GLOBAL
            else -> null
        }
    }

    fun activeModuleLabels(modules: EnabledModules): List<String> = buildList {
        if (modules.career) add("Career")
        if (modules.workout) add("Workout")
        if (modules.diet) add("Diet")
    }

    fun activeModulesSummary(modules: EnabledModules): String =
        activeModuleLabels(modules).joinToString(" · ").ifBlank { "None" }
}

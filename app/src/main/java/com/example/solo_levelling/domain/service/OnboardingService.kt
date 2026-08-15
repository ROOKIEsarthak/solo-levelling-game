package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.seed.SeedData
import com.example.solo_levelling.domain.model.AttributeCode

class OnboardingService(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val questGeneration: QuestGenerationService,
) {
    suspend fun ensureSeeded() {
        if (db.achievementDao().getDefs().isEmpty()) {
            db.achievementDao().upsertDefs(SeedData.achievements())
        }
        if (db.questDao().getActiveTemplates().isEmpty() && db.questDao().getTemplateByKey("recovery") == null) {
            db.questDao().upsertTemplates(SeedData.defaultTemplates())
        }
        if (db.playerDao().getAttributes().isEmpty()) {
            db.playerDao().upsertAttributes(
                AttributeCode.entries.map { AttributeStatEntity(code = it.name) },
            )
        }
        if (db.playerDao().getStreak(SystemDefaults.PLAYER_ID) == null) {
            db.playerDao().upsertStreak(StreakStateEntity())
        }
        if (db.playerDao().getProfile(SystemDefaults.PLAYER_ID) == null) {
            db.playerDao().upsertProfile(
                PlayerProfileEntity(createdAtEpochMs = clock.nowEpochMs()),
            )
        }
        ensureConfigDefaults()
        if (db.moduleDao().getCareerNodes().isEmpty()) {
            SeedData.careerNodes().forEach { node ->
                val status = if (node.orderIndex == 1) "STARTED" else node.status
                db.moduleDao().upsertCareerNode(node.copy(status = status))
            }
        }
    }

    private suspend fun ensureConfigDefaults() {
        val defaults = mapOf(
            "calorie_target" to "2200",
            "protein_target" to "150",
            "step_target" to "10000",
            "notifications_enabled" to "true",
            "schedule_days_csv" to "1,2,3,4,5,6,7",
            "goal_title" to "",
            "schedule_json" to """{"wake":"07:00","sleep":"23:00"}""",
        )
        for ((key, value) in defaults) {
            if (db.configDao().get(key) == null) {
                db.configDao().upsert(UserConfigEntity(key, value))
            }
        }
    }

    suspend fun completeOnboarding(name: String, priorities: List<String>, scheduleDays: List<String> = emptyList()) {
        ensureSeeded()
        val existing = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
            ?: PlayerProfileEntity(createdAtEpochMs = clock.nowEpochMs())
        db.playerDao().upsertProfile(
            existing.copy(
                name = name.ifBlank { "Hunter" },
                prioritiesCsv = priorities.joinToString(","),
                onboardingDone = true,
            ),
        )
        if (scheduleDays.isNotEmpty()) {
            db.configDao().upsert(
                UserConfigEntity("schedule_days_csv", scheduleDays.joinToString(",")),
            )
        }
        questGeneration.generateForToday(existing.timezone)
    }

    /** Wipes all local progress and returns the player to a fresh seeded state (onboarding required). */
    suspend fun resetAllProgress() {
        db.clearProgressTables()
        ensureSeeded()
    }
}

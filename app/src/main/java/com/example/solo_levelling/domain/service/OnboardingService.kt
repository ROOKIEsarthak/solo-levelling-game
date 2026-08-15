package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.seed.SeedData
import com.example.solo_levelling.domain.model.AttributeCode

class OnboardingService(
    private val db: AppDatabase,
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
    }

    suspend fun completeOnboarding(name: String, priorities: List<String>) {
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
        // Keep recovery template in DB but inactive for daily gen
        questGeneration.generateForToday(existing.timezone)
    }
}

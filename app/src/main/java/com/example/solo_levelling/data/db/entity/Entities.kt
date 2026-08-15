package com.example.solo_levelling.data.db.entity

import com.example.solo_levelling.core.config.SystemDefaults

data class PlayerProfileEntity(
    val id: Long = SystemDefaults.PLAYER_ID,
    val name: String = "Hunter",
    val level: Int = 1,
    val totalXp: Int = 0,
    val rank: String = "E",
    val timezone: String = "Asia/Kolkata",
    val onboardingDone: Boolean = false,
    val prioritiesCsv: String = "",
    val createdAtEpochMs: Long = 0L,
)

data class AttributeStatEntity(
    val code: String,
    val currentValue: Int = 0,
    val lifetimeXp: Int = 0,
)

data class QuestTemplateEntity(
    val id: Long = 0,
    val key: String,
    val type: String,
    val title: String,
    val description: String = "",
    val baseXp: Int,
    val attributeRewardsJson: String,
    val scheduleDaysCsv: String = "1,2,3,4,5,6,7",
    val active: Boolean = true,
    val verificationType: String = "MANUAL",
    val verificationTarget: Float = 0f,
    val verificationUnit: String = "",
    val dependsOnTemplateKey: String = "",
    val priorityTags: String = "",
    val difficulty: Int = 1,
)

data class QuestInstanceEntity(
    val id: Long = 0,
    val templateId: Long,
    val scheduledDate: String,
    val status: String = "AVAILABLE",
    val title: String,
    val type: String,
    val baseXp: Int,
    val attributeRewardsJson: String,
    val completedAtEpochMs: Long? = null,
    val verificationType: String = "MANUAL",
    val verificationTarget: Float = 0f,
    val verificationUnit: String = "",
)

data class XpLedgerEntryEntity(
    val id: Long = 0,
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val metadataJson: String = "{}",
    val createdAtEpochMs: Long,
)

data class AchievementDefEntity(
    val key: String,
    val name: String,
    val description: String,
    val criteriaType: String,
    val criteriaValue: Int,
    val rewardXp: Int = 0,
)

data class PlayerAchievementEntity(
    val achievementKey: String,
    val unlockedAtEpochMs: Long,
)

data class StreakStateEntity(
    val id: Long = SystemDefaults.PLAYER_ID,
    val current: Int = 0,
    val best: Int = 0,
    val lastCompletedDate: String? = null,
    val recoveryUsedThisWeek: Int = 0,
    val weekStartDate: String? = null,
)

data class BossEntity(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val targetValue: Float = 100f,
    val currentValue: Float = 0f,
    val xpReward: Int = 200,
    val status: String = "ACTIVE",
    val deadlineDate: String? = null,
)

data class SkillEntity(
    val id: Long = 0,
    val domain: String,
    val name: String,
    val xp: Int = 0,
    val level: Int = 1,
)

data class DsaProblemEntity(
    val id: Long = 0,
    val title: String,
    val platform: String = "LeetCode",
    val externalId: String = "",
    val difficulty: String = "MEDIUM",
    val topic: String = "",
    val status: String = "NOT_STARTED",
    val attempts: Int = 0,
    val confidence: Int = 0,
    val notes: String = "",
    val timeSpentMinutes: Int = 0,
    val solvedAtEpochMs: Long? = null,
)

data class WorkoutEntity(
    val id: Long = 0,
    val date: String,
    val type: String,
    val durationMinutes: Int = 0,
    val notes: String = "",
    val completed: Boolean = true,
)

data class NutritionLogEntity(
    val date: String,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
)

data class FocusSessionEntity(
    val id: Long = 0,
    val date: String,
    val durationMinutes: Int,
    val label: String = "Deep Work",
    val completedAtEpochMs: Long,
)

data class JournalEntryEntity(
    val date: String,
    val content: String,
    val updatedAtEpochMs: Long,
)

data class MetricLogEntity(
    val id: Long = 0,
    val metricType: String,
    val value: Float,
    val recordedAtEpochMs: Long,
    val date: String,
)

data class UserConfigEntity(
    val key: String,
    val value: String,
)

data class SyncOutboxEntity(
    val id: Long = 0,
    val eventType: String,
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val synced: Boolean = false,
)

data class BossQuestEntity(
    val id: Long = 0,
    val bossId: Long,
    val templateKey: String,
    val weight: Float = 1f,
    val completed: Boolean = false,
)

data class WorkoutExerciseEntity(
    val id: Long = 0,
    val workoutId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float,
    val rir: Int = 0,
)

data class CareerNodeEntity(
    val id: Long = 0,
    val track: String,
    val title: String,
    val status: String = "LOCKED",
    val orderIndex: Int,
    val description: String = "",
)

data class SeasonEntity(
    val id: Long = 0,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String = "ACTIVE",
    val seasonXp: Int = 0,
)

data class DismissedSuggestionEntity(
    val id: Long = 0,
    val suggestionKey: String,
    val dismissedAtEpochMs: Long,
)

data class RoutineLogEntity(
    val id: Long = 0,
    val date: String,
    val kind: String,
    val value: String = "",
    val completedAtEpochMs: Long,
)

package com.example.solo_levelling.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solo_levelling.core.config.SystemDefaults

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Long = SystemDefaults.PLAYER_ID,
    val name: String = "Hunter",
    val level: Int = 1,
    val totalXp: Int = 0,
    val rank: String = "E",
    val timezone: String = "Asia/Kolkata",
    val onboardingDone: Boolean = false,
    val prioritiesCsv: String = "",
    val createdAtEpochMs: Long = 0L,
)

@Entity(tableName = "attribute_stats")
data class AttributeStatEntity(
    @PrimaryKey val code: String,
    val currentValue: Int = 0,
    val lifetimeXp: Int = 0,
)

@Entity(tableName = "quest_templates")
data class QuestTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val type: String,
    val title: String,
    val description: String = "",
    val baseXp: Int,
    val attributeRewardsJson: String,
    val scheduleDaysCsv: String = "1,2,3,4,5,6,7",
    val active: Boolean = true,
    val verificationType: String = "MANUAL",
)

@Entity(
    tableName = "quest_instances",
    indices = [
        Index(value = ["templateId", "scheduledDate"], unique = true),
    ],
)
data class QuestInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val scheduledDate: String,
    val status: String = "AVAILABLE",
    val title: String,
    val type: String,
    val baseXp: Int,
    val attributeRewardsJson: String,
    val completedAtEpochMs: Long? = null,
)

@Entity(
    tableName = "xp_ledger",
    indices = [
        Index(value = ["sourceType", "sourceId"], unique = true),
    ],
)
data class XpLedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val metadataJson: String = "{}",
    val createdAtEpochMs: Long,
)

@Entity(tableName = "achievement_defs")
data class AchievementDefEntity(
    @PrimaryKey val key: String,
    val name: String,
    val description: String,
    val criteriaType: String,
    val criteriaValue: Int,
    val rewardXp: Int = 0,
)

@Entity(tableName = "player_achievements")
data class PlayerAchievementEntity(
    @PrimaryKey val achievementKey: String,
    val unlockedAtEpochMs: Long,
)

@Entity(tableName = "streak_state")
data class StreakStateEntity(
    @PrimaryKey val id: Long = SystemDefaults.PLAYER_ID,
    val current: Int = 0,
    val best: Int = 0,
    val lastCompletedDate: String? = null,
    val recoveryUsedThisWeek: Int = 0,
    val weekStartDate: String? = null,
)

@Entity(tableName = "bosses")
data class BossEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val targetValue: Float = 100f,
    val currentValue: Float = 0f,
    val xpReward: Int = 200,
    val status: String = "ACTIVE",
    val deadlineDate: String? = null,
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val name: String,
    val xp: Int = 0,
    val level: Int = 1,
)

@Entity(
    tableName = "dsa_problems",
    indices = [Index(value = ["platform", "externalId"], unique = true)],
)
data class DsaProblemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val platform: String = "LeetCode",
    val externalId: String = "",
    val difficulty: String = "MEDIUM",
    val topic: String = "",
    val status: String = "NOT_STARTED",
    val attempts: Int = 0,
    val confidence: Int = 0,
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String,
    val durationMinutes: Int = 0,
    val notes: String = "",
)

@Entity(tableName = "nutrition_logs")
data class NutritionLogEntity(
    @PrimaryKey val date: String,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val durationMinutes: Int,
    val label: String = "Deep Work",
    val completedAtEpochMs: Long,
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val date: String,
    val content: String,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "metric_logs")
data class MetricLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metricType: String,
    val value: Float,
    val recordedAtEpochMs: Long,
    val date: String,
)

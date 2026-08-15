package com.example.solo_levelling.data.db

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity

internal data class UserProfileJson(
    val id: Long = SystemDefaults.PLAYER_ID,
    val name: String = "Hunter",
    val timezone: String = "Asia/Kolkata",
    val onboardingDone: Boolean = false,
    val prioritiesCsv: String = "",
    val createdAtEpochMs: Long = 0L,
)

internal data class UserJson(
    val profile: UserProfileJson? = null,
    val configs: List<UserConfigEntity> = emptyList(),
)

internal data class NextIdsJson(
    val questTemplate: Long = 1,
    val questInstance: Long = 1,
    val xpLedger: Long = 1,
    val boss: Long = 1,
    val skill: Long = 1,
    val dsa: Long = 1,
    val workout: Long = 1,
    val focus: Long = 1,
    val metric: Long = 1,
    val bossQuest: Long = 1,
    val workoutExercise: Long = 1,
    val careerNode: Long = 1,
    val season: Long = 1,
    val dismissed: Long = 1,
    val routine: Long = 1,
    val syncOutbox: Long = 1,
    val plannedExercise: Long = 1,
    val loggedExercise: Long = 1,
    val dietMeal: Long = 1,
    val dietFood: Long = 1,
)

internal data class ProgressJson(
    val level: Int = 1,
    val totalXp: Int = 0,
    val rank: String = "E",
    val attributes: List<AttributeStatEntity> = emptyList(),
    val streak: StreakStateEntity? = null,
    val nextIds: NextIdsJson = NextIdsJson(),
)

internal data class AchievementsJson(
    val defs: List<AchievementDefEntity> = emptyList(),
    val unlocked: List<PlayerAchievementEntity> = emptyList(),
)

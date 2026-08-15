package com.example.solo_levelling.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: Long): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: Long): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Update
    suspend fun updateProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM attribute_stats")
    fun observeAttributes(): Flow<List<AttributeStatEntity>>

    @Query("SELECT * FROM attribute_stats")
    suspend fun getAttributes(): List<AttributeStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttributes(stats: List<AttributeStatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttribute(stat: AttributeStatEntity)

    @Query("SELECT * FROM streak_state WHERE id = :id LIMIT 1")
    fun observeStreak(id: Long): Flow<StreakStateEntity?>

    @Query("SELECT * FROM streak_state WHERE id = :id LIMIT 1")
    suspend fun getStreak(id: Long): StreakStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: StreakStateEntity)
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quest_templates WHERE active = 1")
    suspend fun getActiveTemplates(): List<QuestTemplateEntity>

    @Query("SELECT * FROM quest_templates WHERE `key` = :key LIMIT 1")
    suspend fun getTemplateByKey(key: String): QuestTemplateEntity?

    @Query("SELECT * FROM quest_templates")
    fun observeTemplates(): Flow<List<QuestTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplates(templates: List<QuestTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: QuestTemplateEntity): Long

    @Query("SELECT * FROM quest_instances WHERE scheduledDate = :date ORDER BY id ASC")
    fun observeInstancesForDate(date: String): Flow<List<QuestInstanceEntity>>

    @Query("SELECT * FROM quest_instances WHERE scheduledDate = :date ORDER BY id ASC")
    suspend fun getInstancesForDate(date: String): List<QuestInstanceEntity>

    @Query("SELECT * FROM quest_instances WHERE id = :id LIMIT 1")
    suspend fun getInstance(id: Long): QuestInstanceEntity?

    @Query("SELECT * FROM quest_instances WHERE type = 'WEEKLY' AND scheduledDate >= :weekStart AND scheduledDate <= :weekEnd")
    suspend fun getWeeklyInstances(weekStart: String, weekEnd: String): List<QuestInstanceEntity>

    @Query(
        "SELECT COUNT(*) FROM quest_instances WHERE scheduledDate >= :weekStart AND scheduledDate <= :weekEnd AND status = 'COMPLETED'",
    )
    suspend fun countCompletedInRange(weekStart: String, weekEnd: String): Int

    @Query(
        "SELECT COUNT(*) FROM quest_instances WHERE scheduledDate >= :weekStart AND scheduledDate <= :weekEnd",
    )
    suspend fun countTotalInRange(weekStart: String, weekEnd: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInstance(instance: QuestInstanceEntity): Long

    @Update
    suspend fun updateInstance(instance: QuestInstanceEntity)

    @Query("SELECT COUNT(*) FROM quest_instances WHERE status = 'COMPLETED'")
    suspend fun countCompletedAll(): Int
}

@Dao
interface XpDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLedger(entry: XpLedgerEntryEntity): Long

    @Query("SELECT * FROM xp_ledger WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun findBySource(sourceType: String, sourceId: String): XpLedgerEntryEntity?

    @Query("SELECT * FROM xp_ledger ORDER BY createdAtEpochMs DESC")
    fun observeLedger(): Flow<List<XpLedgerEntryEntity>>

    @Query("SELECT * FROM xp_ledger ORDER BY createdAtEpochMs ASC")
    suspend fun getAllLedger(): List<XpLedgerEntryEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger")
    suspend fun sumXp(): Int

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM xp_ledger WHERE createdAtEpochMs >= :startMs AND createdAtEpochMs < :endMs AND amount > 0",
    )
    suspend fun sumXpBetween(startMs: Long, endMs: Long): Int
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDefs(defs: List<AchievementDefEntity>)

    @Query("SELECT * FROM achievement_defs")
    fun observeDefs(): Flow<List<AchievementDefEntity>>

    @Query("SELECT * FROM achievement_defs")
    suspend fun getDefs(): List<AchievementDefEntity>

    @Query("SELECT * FROM player_achievements")
    fun observeUnlocked(): Flow<List<PlayerAchievementEntity>>

    @Query("SELECT * FROM player_achievements")
    suspend fun getUnlocked(): List<PlayerAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(achievement: PlayerAchievementEntity)
}

@Dao
interface ModuleDao {
    @Query("SELECT * FROM bosses ORDER BY id DESC")
    fun observeBosses(): Flow<List<BossEntity>>

    @Query("SELECT * FROM bosses WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveBoss(): BossEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBoss(boss: BossEntity): Long

    @Update
    suspend fun updateBoss(boss: BossEntity)

    @Query("SELECT * FROM skills ORDER BY domain, name")
    fun observeSkills(): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkill(skill: SkillEntity): Long

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Query("SELECT * FROM dsa_problems ORDER BY id DESC")
    fun observeDsa(): Flow<List<DsaProblemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDsa(problem: DsaProblemEntity): Long

    @Update
    suspend fun updateDsa(problem: DsaProblemEntity)

    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("SELECT * FROM nutrition_logs WHERE date = :date LIMIT 1")
    fun observeNutrition(date: String): Flow<NutritionLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutrition(log: NutritionLogEntity)

    @Query("SELECT * FROM focus_sessions WHERE date = :date ORDER BY id DESC")
    fun observeFocus(date: String): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insertFocus(session: FocusSessionEntity): Long

    @Query("SELECT * FROM journal_entries WHERE date = :date LIMIT 1")
    fun observeJournal(date: String): Flow<JournalEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournal(entry: JournalEntryEntity)

    @Insert
    suspend fun insertMetric(metric: MetricLogEntity): Long

    @Query("SELECT * FROM metric_logs WHERE metricType = :type ORDER BY recordedAtEpochMs DESC LIMIT :limit")
    suspend fun recentMetrics(type: String, limit: Int): List<MetricLogEntity>

    @Query("SELECT COUNT(*) FROM dsa_problems WHERE status IN ('SOLVED','MASTERED')")
    suspend fun countDsaSolved(): Int

    @Query("SELECT COUNT(*) FROM bosses WHERE status = 'CLEARED'")
    suspend fun countBossCleared(): Int

    @Query("SELECT * FROM dsa_problems WHERE id = :id LIMIT 1")
    suspend fun getDsa(id: Long): DsaProblemEntity?

    @Query("SELECT * FROM skills WHERE domain = :domain AND name = :name LIMIT 1")
    suspend fun findSkill(domain: String, name: String): SkillEntity?
}

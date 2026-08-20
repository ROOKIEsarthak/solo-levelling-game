package com.example.solo_levelling.data.db.dao

import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.DismissedSuggestionEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.RoutineLogEntity
import com.example.solo_levelling.data.db.entity.SeasonEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.SyncOutboxEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import kotlinx.coroutines.flow.Flow

interface PlayerDao {
    fun observeProfile(id: Long): Flow<PlayerProfileEntity?>

    suspend fun getProfile(id: Long): PlayerProfileEntity?

    suspend fun upsertProfile(profile: PlayerProfileEntity)

    suspend fun updateProfile(profile: PlayerProfileEntity)

    fun observeAttributes(): Flow<List<AttributeStatEntity>>

    suspend fun getAttributes(): List<AttributeStatEntity>

    suspend fun upsertAttributes(stats: List<AttributeStatEntity>)

    suspend fun upsertAttribute(stat: AttributeStatEntity)

    fun observeStreak(id: Long): Flow<StreakStateEntity?>

    suspend fun getStreak(id: Long): StreakStateEntity?

    suspend fun upsertStreak(streak: StreakStateEntity)
}

interface QuestDao {
    suspend fun getActiveTemplates(): List<QuestTemplateEntity>

    suspend fun getTemplateByKey(key: String): QuestTemplateEntity?

    suspend fun getTemplateById(id: Long): QuestTemplateEntity?

    suspend fun countInstancesForTemplate(templateId: Long): Int

    suspend fun getInstancesDependingOn(date: String, dependsOnKey: String): List<QuestInstanceEntity>

    fun observeTemplates(): Flow<List<QuestTemplateEntity>>

    suspend fun upsertTemplates(templates: List<QuestTemplateEntity>)

    suspend fun upsertTemplate(template: QuestTemplateEntity): Long

    fun observeInstancesForDate(date: String): Flow<List<QuestInstanceEntity>>

    suspend fun getInstancesForDate(date: String): List<QuestInstanceEntity>

    suspend fun getInstance(id: Long): QuestInstanceEntity?

    suspend fun getWeeklyInstances(weekStart: String, weekEnd: String): List<QuestInstanceEntity>

    suspend fun countCompletedInRange(weekStart: String, weekEnd: String): Int

    suspend fun countTotalInRange(weekStart: String, weekEnd: String): Int

    suspend fun countIncompleteInRange(weekStart: String, weekEnd: String): Int

    suspend fun insertInstance(instance: QuestInstanceEntity): Long

    suspend fun updateInstance(instance: QuestInstanceEntity)

    suspend fun getAllInstances(): List<QuestInstanceEntity>

    suspend fun deleteInstance(id: Long)

    suspend fun countCompletedAll(): Int

    suspend fun getInstancesBeforeDate(date: String): List<QuestInstanceEntity>

    fun observeInstancesByType(type: String): Flow<List<QuestInstanceEntity>>

    suspend fun getInstancesInRange(startDate: String, endDate: String): List<QuestInstanceEntity>

    suspend fun updateTemplate(template: QuestTemplateEntity)
}

interface XpDao {
    suspend fun insertLedger(entry: XpLedgerEntryEntity): Long

    suspend fun findBySource(sourceType: String, sourceId: String): XpLedgerEntryEntity?

    fun observeLedger(): Flow<List<XpLedgerEntryEntity>>

    suspend fun getAllLedger(): List<XpLedgerEntryEntity>

    suspend fun sumXp(): Int

    suspend fun sumXpBetween(startMs: Long, endMs: Long): Int
}

interface ConfigDao {
    suspend fun get(key: String): UserConfigEntity?

    fun observe(key: String): Flow<UserConfigEntity?>

    suspend fun upsert(config: UserConfigEntity)
}

interface OutboxDao {
    suspend fun insert(entry: SyncOutboxEntity): Long

    fun observeRecent(limit: Int): Flow<List<SyncOutboxEntity>>

    suspend fun markSynced(ids: List<Long>)

    suspend fun getUnsynced(): List<SyncOutboxEntity>
}

interface AchievementDao {
    suspend fun upsertDefs(defs: List<AchievementDefEntity>)

    fun observeDefs(): Flow<List<AchievementDefEntity>>

    suspend fun getDefs(): List<AchievementDefEntity>

    fun observeUnlocked(): Flow<List<PlayerAchievementEntity>>

    suspend fun getUnlocked(): List<PlayerAchievementEntity>

    suspend fun unlock(achievement: PlayerAchievementEntity)
}

interface ModuleDao {
    fun observeBosses(): Flow<List<BossEntity>>

    suspend fun getActiveBoss(): BossEntity?

    suspend fun getBosses(): List<BossEntity>

    suspend fun upsertBoss(boss: BossEntity): Long

    suspend fun updateBoss(boss: BossEntity)

    fun observeSkills(): Flow<List<SkillEntity>>

    suspend fun upsertSkill(skill: SkillEntity): Long

    suspend fun updateSkill(skill: SkillEntity)

    fun observeDsa(): Flow<List<DsaProblemEntity>>

    suspend fun upsertDsa(problem: DsaProblemEntity): Long

    suspend fun updateDsa(problem: DsaProblemEntity)

    fun observeWorkouts(): Flow<List<WorkoutLogEntity>>

    fun observeWorkoutLog(date: String): Flow<WorkoutLogEntity?>

    suspend fun getWorkoutLog(date: String): WorkoutLogEntity?

    suspend fun upsertWorkoutLog(log: WorkoutLogEntity): Long

    suspend fun getAllWorkoutLogs(): List<WorkoutLogEntity>

    suspend fun deleteWorkoutLog(date: String)

    fun observeWorkoutRoutine(): Flow<WorkoutRoutineEntity>

    suspend fun getWorkoutRoutine(): WorkoutRoutineEntity

    suspend fun upsertWorkoutRoutine(routine: WorkoutRoutineEntity)

    fun observeDietLog(date: String): Flow<DietLogEntity?>

    fun observeDietLogs(): Flow<List<DietLogEntity>>

    suspend fun getDietLog(date: String): DietLogEntity?

    suspend fun upsertDietLog(log: DietLogEntity)

    suspend fun deleteDietLog(date: String)

    fun observeNutrition(date: String): Flow<NutritionLogEntity?>

    suspend fun upsertNutrition(log: NutritionLogEntity)

    fun observeFocus(date: String): Flow<List<FocusSessionEntity>>

    suspend fun insertFocus(session: FocusSessionEntity): Long

    fun observeJournal(date: String): Flow<JournalEntryEntity?>

    suspend fun getJournal(date: String): JournalEntryEntity?

    suspend fun upsertJournal(entry: JournalEntryEntity)

    suspend fun insertMetric(metric: MetricLogEntity): Long

    suspend fun recentMetrics(type: String, limit: Int): List<MetricLogEntity>

    fun observeRecentMetrics(limit: Int): Flow<List<MetricLogEntity>>

    suspend fun countDsaSolved(): Int

    suspend fun countBossCleared(): Int

    suspend fun getDsa(id: Long): DsaProblemEntity?

    suspend fun findSkill(domain: String, name: String): SkillEntity?

    suspend fun upsertBossQuest(quest: BossQuestEntity): Long

    suspend fun getBossQuests(bossId: Long): List<BossQuestEntity>

    suspend fun updateBossQuest(quest: BossQuestEntity)

    suspend fun upsertCareerNode(node: CareerNodeEntity): Long

    fun observeCareerNodes(): Flow<List<CareerNodeEntity>>

    suspend fun getCareerNodes(): List<CareerNodeEntity>

    suspend fun getCareerNode(id: Long): CareerNodeEntity?

    suspend fun upsertSeason(season: SeasonEntity): Long

    suspend fun getActiveSeason(): SeasonEntity?

    fun observeActiveSeason(): Flow<SeasonEntity?>

    suspend fun insertDismissedSuggestion(suggestion: DismissedSuggestionEntity): Long

    suspend fun findDismissedSuggestion(key: String): DismissedSuggestionEntity?

    suspend fun getDismissedSuggestionKeys(): List<String>

    suspend fun countWorkoutsInRange(startDate: String, endDate: String): Int

    suspend fun countWorkoutDaysInRange(startDate: String, endDate: String): Int

    suspend fun countDsaSolvedInRange(startMs: Long, endMs: Long): Int

    suspend fun insertRoutineLog(log: RoutineLogEntity): Long

    suspend fun getRoutineLogsForDate(date: String): List<RoutineLogEntity>

    fun observeRoutineLogs(date: String): Flow<List<RoutineLogEntity>>

    suspend fun sumMetricForDate(date: String, type: String): Float

    suspend fun sumFocusMinutes(date: String): Int

    suspend fun countDsaSolvedOnDate(dayStartMs: Long, dayEndMs: Long): Int

    suspend fun getNutrition(date: String): NutritionLogEntity?

    fun observeBossQuests(bossId: Long): Flow<List<BossQuestEntity>>

    fun observeSystemDesignTopics(): Flow<List<SystemDesignTopicEntity>>

    suspend fun getSystemDesignTopics(): List<SystemDesignTopicEntity>

    suspend fun upsertSystemDesignTopic(topic: SystemDesignTopicEntity)

    suspend fun replaceSystemDesignTopics(topics: List<SystemDesignTopicEntity>)

    suspend fun getDsaProblems(): List<DsaProblemEntity>
}

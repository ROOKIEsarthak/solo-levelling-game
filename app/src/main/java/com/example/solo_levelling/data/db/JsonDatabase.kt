package com.example.solo_levelling.data.db

import android.database.sqlite.SQLiteConstraintException
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.dao.AchievementDao
import com.example.solo_levelling.data.db.dao.ConfigDao
import com.example.solo_levelling.data.db.dao.ModuleDao
import com.example.solo_levelling.data.db.dao.OutboxDao
import com.example.solo_levelling.data.db.dao.PlayerDao
import com.example.solo_levelling.data.db.dao.QuestDao
import com.example.solo_levelling.data.db.dao.XpDao
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.DismissedSuggestionEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.RoutineLogEntity
import com.example.solo_levelling.data.db.entity.SeasonEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.SyncOutboxEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import com.example.solo_levelling.data.db.entity.WorkoutExerciseEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private class TransactionContext : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key

    companion object Key : CoroutineContext.Key<TransactionContext>
}

class JsonDatabase(private val rootDir: File) {
    private val io = JsonFileIO(rootDir)
    private val gson = Gson()
    private val writeMutex = Mutex()

    private var userJson = UserJson()
    private var progressJson = ProgressJson()
    private var xpLedger = mutableListOf<XpLedgerEntryEntity>()
    private var achievementsJson = AchievementsJson()
    private var questTemplates = mutableListOf<QuestTemplateEntity>()
    private var questInstances = mutableListOf<QuestInstanceEntity>()
    private var seasons = mutableListOf<SeasonEntity>()
    private var skills = mutableListOf<SkillEntity>()
    private var bosses = mutableListOf<BossEntity>()
    private var bossQuests = mutableListOf<BossQuestEntity>()
    private var careerNodes = mutableListOf<CareerNodeEntity>()
    private var syncOutbox = mutableListOf<SyncOutboxEntity>()
    private var dismissed = mutableListOf<DismissedSuggestionEntity>()
    private var workoutRoutine = WorkoutRoutineEntity()
    private var workoutLogs = mutableMapOf<String, WorkoutLogEntity>()
    private var dietLogs = mutableMapOf<String, DietLogEntity>()
    private var focus = mutableListOf<FocusSessionEntity>()
    private var journal = mutableListOf<JournalEntryEntity>()
    private var metrics = mutableListOf<MetricLogEntity>()
    private var routines = mutableListOf<RoutineLogEntity>()
    private var dsa = mutableListOf<DsaProblemEntity>()
    private var systemDesignTopics = mutableListOf<SystemDesignTopicEntity>()

    private val profileFlow = MutableStateFlow<PlayerProfileEntity?>(null)
    private val attributesFlow = MutableStateFlow<List<AttributeStatEntity>>(emptyList())
    private val streakFlow = MutableStateFlow<StreakStateEntity?>(null)
    private val templatesFlow = MutableStateFlow<List<QuestTemplateEntity>>(emptyList())
    private val instancesFlow = MutableStateFlow<List<QuestInstanceEntity>>(emptyList())
    private val ledgerFlow = MutableStateFlow<List<XpLedgerEntryEntity>>(emptyList())
    private val achievementDefsFlow = MutableStateFlow<List<AchievementDefEntity>>(emptyList())
    private val unlockedFlow = MutableStateFlow<List<PlayerAchievementEntity>>(emptyList())
    private val bossesFlow = MutableStateFlow<List<BossEntity>>(emptyList())
    private val skillsFlow = MutableStateFlow<List<SkillEntity>>(emptyList())
    private val dsaFlow = MutableStateFlow<List<DsaProblemEntity>>(emptyList())
    private val systemDesignTopicsFlow = MutableStateFlow<List<SystemDesignTopicEntity>>(emptyList())
    private val workoutsFlow = MutableStateFlow<List<WorkoutLogEntity>>(emptyList())
    private val workoutRoutineFlow = MutableStateFlow(WorkoutRoutineEntity())
    private val dietLogsFlow = MutableStateFlow<List<DietLogEntity>>(emptyList())
    private val careerNodesFlow = MutableStateFlow<List<CareerNodeEntity>>(emptyList())
    private val activeSeasonFlow = MutableStateFlow<SeasonEntity?>(null)
    private val recentMetricsFlow = MutableStateFlow<List<MetricLogEntity>>(emptyList())
    private val configFlows = mutableMapOf<String, MutableStateFlow<UserConfigEntity?>>()
    private val nutritionFlows = mutableMapOf<String, MutableStateFlow<NutritionLogEntity?>>()
    private val dietLogFlows = mutableMapOf<String, MutableStateFlow<DietLogEntity?>>()
    private val workoutLogFlows = mutableMapOf<String, MutableStateFlow<WorkoutLogEntity?>>()
    private val focusFlows = mutableMapOf<String, MutableStateFlow<List<FocusSessionEntity>>>()
    private val journalFlows = mutableMapOf<String, MutableStateFlow<JournalEntryEntity?>>()
    private val routineFlows = mutableMapOf<String, MutableStateFlow<List<RoutineLogEntity>>>()
    private val bossQuestFlows = mutableMapOf<Long, MutableStateFlow<List<BossQuestEntity>>>()
    private val outboxFlows = mutableMapOf<Int, MutableStateFlow<List<SyncOutboxEntity>>>()

    private val playerDaoImpl = PlayerDaoImpl()
    private val questDaoImpl = QuestDaoImpl()
    private val xpDaoImpl = XpDaoImpl()
    private val configDaoImpl = ConfigDaoImpl()
    private val outboxDaoImpl = OutboxDaoImpl()
    private val achievementDaoImpl = AchievementDaoImpl()
    private val moduleDaoImpl = ModuleDaoImpl()

    init {
        io.ensureRoot()
        loadAll()
        emitAllFlows()
    }

    fun playerDao(): PlayerDao = playerDaoImpl
    fun questDao(): QuestDao = questDaoImpl
    fun xpDao(): XpDao = xpDaoImpl
    fun achievementDao(): AchievementDao = achievementDaoImpl
    fun moduleDao(): ModuleDao = moduleDaoImpl
    fun configDao(): ConfigDao = configDaoImpl
    fun outboxDao(): OutboxDao = outboxDaoImpl

    suspend fun <R> withTransaction(block: suspend () -> R): R {
        val context = if (isOnMainDispatcher()) {
            Dispatchers.IO + TransactionContext()
        } else {
            TransactionContext()
        }
        return withContext(context) {
            writeMutex.withLock { block() }
        }
    }

    suspend fun clearProgressTables() = withWriteLock {
        val savedProfile = userJson.profile
        val savedConfigs = userJson.configs
        userJson = UserJson(
            profile = UserProfileJson(
                id = SystemDefaults.PLAYER_ID,
                name = savedProfile?.name ?: "Hunter",
                timezone = savedProfile?.timezone ?: "Asia/Kolkata",
                onboardingDone = false,
                prioritiesCsv = savedProfile?.prioritiesCsv ?: "",
                createdAtEpochMs = savedProfile?.createdAtEpochMs ?: 0L,
            ),
            configs = savedConfigs,
        )
        progressJson = ProgressJson()
        xpLedger.clear()
        achievementsJson = AchievementsJson()
        questTemplates.clear()
        questInstances.clear()
        seasons.clear()
        skills.clear()
        bosses.clear()
        bossQuests.clear()
        careerNodes.clear()
        syncOutbox.clear()
        dismissed.clear()
        workoutLogs.clear()
        dietLogs.clear()
        focus.clear()
        journal.clear()
        metrics.clear()
        routines.clear()
        dsa.clear()
        systemDesignTopics.clear()
        io.clearTasks()
        io.clearDir(JsonFileIO.WORKOUTS_LOGS_DIR)
        io.clearDir(JsonFileIO.DIET_LOGS_DIR)
        persistAll()
        emitAllFlows()
    }

    fun close() {}

    private suspend fun <T> withWriteLock(block: suspend () -> T): T {
        if (coroutineContext[TransactionContext.Key] != null) return block()
        return if (isOnMainDispatcher()) {
            withContext(Dispatchers.IO) {
                writeMutex.withLock { block() }
            }
        } else {
            writeMutex.withLock { block() }
        }
    }

    private suspend fun isOnMainDispatcher(): Boolean {
        val interceptor = coroutineContext[ContinuationInterceptor]
        return interceptor === Dispatchers.Main || interceptor === Dispatchers.Main.immediate
    }

    private inline fun <reified T> readList(name: String): MutableList<T> {
        val json = io.readText(name) ?: return mutableListOf()
        if (json.isBlank()) return mutableListOf()
        val type = object : TypeToken<List<T>>() {}.type
        return (gson.fromJson<List<T>>(json, type) ?: emptyList()).toMutableList()
    }

    private inline fun <reified T> writeList(name: String, list: List<T>) {
        io.writeText(name, gson.toJson(list))
    }

    private fun loadAll() {
        userJson = io.readText(FILE_USER)?.let { gson.fromJson(it, UserJson::class.java) } ?: UserJson()
        progressJson = io.readText(FILE_PROGRESS)?.let { gson.fromJson(it, ProgressJson::class.java) } ?: ProgressJson()
        xpLedger = readList(FILE_XP_LEDGER)
        achievementsJson = io.readText(FILE_ACHIEVEMENTS)?.let { gson.fromJson(it, AchievementsJson::class.java) }
            ?: AchievementsJson()
        questTemplates = readList(FILE_QUEST_TEMPLATES)
        seasons = readList(FILE_SEASONS)
        skills = readList(FILE_SKILLS)
        bosses = readList(FILE_BOSSES)
        bossQuests = readList(FILE_BOSS_QUESTS)
        careerNodes = readList(FILE_CAREER_NODES)
        syncOutbox = readList(FILE_SYNC_OUTBOX)
        dismissed = readList(FILE_DISMISSED)
        focus = readList(FILE_FOCUS)
        journal = readList(FILE_JOURNAL)
        metrics = readList(FILE_METRICS)
        routines = readList(FILE_ROUTINES)
        dsa = readList(FILE_DSA)
        systemDesignTopics = readList(FILE_SYSTEM_DESIGN_TOPICS)
        questInstances = io.listTasks().mapNotNull { file ->
            runCatching {
                gson.fromJson(file.readText(), QuestInstanceEntity::class.java)
            }.getOrNull()
        }.toMutableList()
        loadWorkoutAndDiet()
    }

    private fun loadWorkoutAndDiet() {
        workoutRoutine = io.readText(JsonFileIO.WORKOUT_ROUTINE_FILE)
            ?.let { runCatching { gson.fromJson(it, WorkoutRoutineEntity::class.java) }.getOrNull() }
            ?: WorkoutRoutineEntity()
        workoutLogs.clear()
        io.listJsonFiles(JsonFileIO.WORKOUTS_LOGS_DIR).forEach { file ->
            runCatching {
                gson.fromJson(file.readText(), WorkoutLogEntity::class.java)
            }.getOrNull()?.let { log -> workoutLogs[log.date] = log }
        }
        dietLogs.clear()
        io.listJsonFiles(JsonFileIO.DIET_LOGS_DIR).forEach { file ->
            runCatching {
                gson.fromJson(file.readText(), DietLogEntity::class.java)
            }.getOrNull()?.let { log -> dietLogs[log.date] = log }
        }
        migrateLegacyFitnessIfNeeded()
    }

    private fun migrateLegacyFitnessIfNeeded() {
        val legacyWorkouts = readList<WorkoutEntity>(FILE_WORKOUTS)
        val legacyExercises = readList<WorkoutExerciseEntity>(FILE_WORKOUT_EXERCISES)
        val legacyNutrition = readList<NutritionLogEntity>(FILE_NUTRITION)
        if (workoutLogs.isEmpty() && legacyWorkouts.isNotEmpty()) {
            val byWorkout = legacyExercises.groupBy { it.workoutId }
            for (w in legacyWorkouts) {
                val exercises = byWorkout[w.id].orEmpty().map { ex ->
                    LoggedExerciseEntity(
                        id = ex.id,
                        name = ex.name,
                        sets = List(ex.sets.coerceAtLeast(1)) {
                            LoggedSetEntity(weight = ex.weightKg, reps = ex.reps)
                        },
                    )
                }
                workoutLogs[w.date] = WorkoutLogEntity(
                    id = w.id,
                    date = w.date,
                    dayOfWeek = dayOfWeekForDate(w.date),
                    workoutName = w.type,
                    durationMinutes = w.durationMinutes,
                    notes = w.notes,
                    exercises = exercises,
                )
            }
            workoutLogs.values.forEach { persistWorkoutLogFile(it) }
            io.delete(FILE_WORKOUTS)
            io.delete(FILE_WORKOUT_EXERCISES)
        }
        if (dietLogs.isEmpty() && legacyNutrition.isNotEmpty()) {
            for (n in legacyNutrition) {
                val food = FoodItemEntity(
                    id = 1,
                    name = "Logged macros",
                    calories = n.calories,
                    protein = n.protein,
                    carbs = n.carbs,
                    fat = n.fat,
                )
                val meal = MealEntity(id = 1, name = "Logged macros", foods = listOf(food))
                dietLogs[n.date] = DietLogEntity(
                    date = n.date,
                    meals = listOf(meal),
                    dailyTotals = NutritionTotalsEntity(n.calories, n.protein, n.carbs, n.fat),
                )
            }
            dietLogs.values.forEach { persistDietLogFile(it) }
            io.delete(FILE_NUTRITION)
        }
    }

    private fun dayOfWeekForDate(date: String): String =
        runCatching {
            LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
        }.getOrDefault("")

    private fun nutritionFromDiet(date: String): NutritionLogEntity? {
        val log = dietLogs[date] ?: return null
        val t = log.dailyTotals
        return NutritionLogEntity(date, t.calories, t.protein, t.carbs, t.fat)
    }

    private fun persistWorkoutLogFile(log: WorkoutLogEntity) {
        io.writeText("${JsonFileIO.WORKOUTS_LOGS_DIR}/${log.date}.json", gson.toJson(log))
    }

    private fun persistDietLogFile(log: DietLogEntity) {
        io.writeText("${JsonFileIO.DIET_LOGS_DIR}/${log.date}.json", gson.toJson(log))
    }

    private fun persistWorkoutRoutine() {
        io.writeText(JsonFileIO.WORKOUT_ROUTINE_FILE, gson.toJson(workoutRoutine))
    }

    private fun emitWorkoutFlows() {
        workoutsFlow.value = workoutLogs.values.sortedByDescending { it.date }
        workoutLogFlows.forEach { (date, flow) -> flow.value = workoutLogs[date] }
    }

    private fun emitDietFlows() {
        dietLogsFlow.value = dietLogs.values.sortedByDescending { it.date }
        dietLogFlows.forEach { (date, flow) -> flow.value = dietLogs[date] }
        nutritionFlows.forEach { (date, flow) -> flow.value = nutritionFromDiet(date) }
    }

    private fun persistAll() {
        io.writeText(FILE_USER, gson.toJson(userJson))
        io.writeText(FILE_PROGRESS, gson.toJson(progressJson))
        io.writeText(FILE_XP_LEDGER, gson.toJson(xpLedger))
        io.writeText(FILE_ACHIEVEMENTS, gson.toJson(achievementsJson))
        io.writeText(FILE_QUEST_TEMPLATES, gson.toJson(questTemplates))
        io.writeText(FILE_SEASONS, gson.toJson(seasons))
        io.writeText(FILE_SKILLS, gson.toJson(skills))
        io.writeText(FILE_BOSSES, gson.toJson(bosses))
        io.writeText(FILE_BOSS_QUESTS, gson.toJson(bossQuests))
        io.writeText(FILE_CAREER_NODES, gson.toJson(careerNodes))
        io.writeText(FILE_SYNC_OUTBOX, gson.toJson(syncOutbox))
        io.writeText(FILE_DISMISSED, gson.toJson(dismissed))
        persistWorkoutRoutine()
        workoutLogs.values.forEach { persistWorkoutLogFile(it) }
        dietLogs.values.forEach { persistDietLogFile(it) }
        io.writeText(FILE_FOCUS, gson.toJson(focus))
        io.writeText(FILE_JOURNAL, gson.toJson(journal))
        io.writeText(FILE_METRICS, gson.toJson(metrics))
        io.writeText(FILE_ROUTINES, gson.toJson(routines))
        io.writeText(FILE_DSA, gson.toJson(dsa))
        io.writeText(FILE_SYSTEM_DESIGN_TOPICS, gson.toJson(systemDesignTopics))
        persistTasks()
    }

    private fun persistUser() = io.writeText(FILE_USER, gson.toJson(userJson))
    private fun persistProgress() = io.writeText(FILE_PROGRESS, gson.toJson(progressJson))
    private fun persistXpLedger() = io.writeText(FILE_XP_LEDGER, gson.toJson(xpLedger))
    private fun persistAchievements() = io.writeText(FILE_ACHIEVEMENTS, gson.toJson(achievementsJson))
    private fun persistQuestTemplates() = io.writeText(FILE_QUEST_TEMPLATES, gson.toJson(questTemplates))
    private fun persistTasks() {
        val ids = questInstances.map { it.id }.toSet()
        io.listTasks().forEach { file ->
            val id = file.name.removePrefix(JsonFileIO.TASK_PREFIX).removeSuffix(".json").toLongOrNull()
            if (id != null && id !in ids) io.deleteTask(id)
        }
        questInstances.forEach { instance ->
            io.writeTask(instance.id, gson.toJson(instance))
        }
    }

    private fun mergeProfile(id: Long): PlayerProfileEntity? {
        val profile = userJson.profile ?: return null
        if (profile.id != id) return null
        return PlayerProfileEntity(
            id = profile.id,
            name = profile.name,
            level = progressJson.level,
            totalXp = progressJson.totalXp,
            rank = progressJson.rank,
            timezone = profile.timezone,
            onboardingDone = profile.onboardingDone,
            prioritiesCsv = profile.prioritiesCsv,
            createdAtEpochMs = profile.createdAtEpochMs,
        )
    }

    private fun nextId(selector: (NextIdsJson) -> Long, updater: (NextIdsJson, Long) -> NextIdsJson): Long {
        val current = selector(progressJson.nextIds)
        val assigned = if (current <= 0) 1L else current
        progressJson = progressJson.copy(nextIds = updater(progressJson.nextIds, assigned + 1))
        return assigned
    }

    private fun emitAllFlows() {
        profileFlow.value = mergeProfile(SystemDefaults.PLAYER_ID)
        attributesFlow.value = progressJson.attributes
        streakFlow.value = progressJson.streak
        templatesFlow.value = questTemplates.toList()
        instancesFlow.value = questInstances.sortedBy { it.id }
        ledgerFlow.value = xpLedger.sortedByDescending { it.createdAtEpochMs }
        achievementDefsFlow.value = achievementsJson.defs
        unlockedFlow.value = achievementsJson.unlocked
        bossesFlow.value = bosses.sortedByDescending { it.id }
        skillsFlow.value = skills.sortedWith(compareBy({ it.domain }, { it.name }))
        dsaFlow.value = dsa.sortedByDescending { it.id }
        systemDesignTopicsFlow.value = systemDesignTopics.sortedBy { it.orderIndex }
        workoutRoutineFlow.value = workoutRoutine
        emitWorkoutFlows()
        emitDietFlows()
        careerNodesFlow.value = careerNodes.sortedWith(compareBy({ it.track }, { it.orderIndex }))
        activeSeasonFlow.value = seasons.firstOrNull { it.status == "ACTIVE" }
        recentMetricsFlow.value = metrics.sortedByDescending { it.recordedAtEpochMs }
        configFlows.values.forEach { flow ->
            val key = flow.value?.key
            flow.value = userJson.configs.firstOrNull { it.key == key }
        }
        focusFlows.forEach { (date, flow) ->
            flow.value = focus.filter { it.date == date }.sortedByDescending { it.id }
        }
        journalFlows.forEach { (date, flow) ->
            flow.value = journal.firstOrNull { it.date == date }
        }
        routineFlows.forEach { (date, flow) ->
            flow.value = routines.filter { it.date == date }.sortedBy { it.id }
        }
        bossQuestFlows.forEach { (bossId, flow) ->
            flow.value = bossQuests.filter { it.bossId == bossId }
        }
        outboxFlows.forEach { (limit, flow) ->
            flow.value = syncOutbox.sortedByDescending { it.createdAtEpochMs }.take(limit)
        }
    }

    private fun configFlow(key: String): MutableStateFlow<UserConfigEntity?> =
        configFlows.getOrPut(key) {
            MutableStateFlow(userJson.configs.firstOrNull { it.key == key })
        }

    private inner class PlayerDaoImpl : PlayerDao {
        override fun observeProfile(id: Long): Flow<PlayerProfileEntity?> =
            profileFlow.map { if (it?.id == id) it else null }

        override suspend fun getProfile(id: Long): PlayerProfileEntity? = withWriteLock {
            mergeProfile(id)
        }

        override suspend fun upsertProfile(profile: PlayerProfileEntity) = withWriteLock {
            userJson = userJson.copy(
                profile = UserProfileJson(
                    id = profile.id,
                    name = profile.name,
                    timezone = profile.timezone,
                    onboardingDone = profile.onboardingDone,
                    prioritiesCsv = profile.prioritiesCsv,
                    createdAtEpochMs = profile.createdAtEpochMs,
                ),
            )
            progressJson = progressJson.copy(
                level = profile.level,
                totalXp = profile.totalXp,
                rank = profile.rank,
            )
            persistUser()
            persistProgress()
            profileFlow.value = mergeProfile(profile.id)
        }

        override suspend fun updateProfile(profile: PlayerProfileEntity) = upsertProfile(profile)

        override fun observeAttributes(): Flow<List<AttributeStatEntity>> = attributesFlow

        override suspend fun getAttributes(): List<AttributeStatEntity> = withWriteLock {
            progressJson.attributes
        }

        override suspend fun upsertAttributes(stats: List<AttributeStatEntity>) = withWriteLock {
            val map = progressJson.attributes.associateBy { it.code }.toMutableMap()
            stats.forEach { map[it.code] = it }
            progressJson = progressJson.copy(attributes = map.values.toList())
            persistProgress()
            attributesFlow.value = progressJson.attributes
        }

        override suspend fun upsertAttribute(stat: AttributeStatEntity) = withWriteLock {
            val list = progressJson.attributes.toMutableList()
            val idx = list.indexOfFirst { it.code == stat.code }
            if (idx >= 0) list[idx] = stat else list.add(stat)
            progressJson = progressJson.copy(attributes = list)
            persistProgress()
            attributesFlow.value = progressJson.attributes
        }

        override fun observeStreak(id: Long): Flow<StreakStateEntity?> =
            streakFlow.map { if (it?.id == id) it else null }

        override suspend fun getStreak(id: Long): StreakStateEntity? = withWriteLock {
            progressJson.streak?.takeIf { it.id == id }
        }

        override suspend fun upsertStreak(streak: StreakStateEntity) = withWriteLock {
            progressJson = progressJson.copy(streak = streak)
            persistProgress()
            streakFlow.value = streak
        }
    }

    private inner class QuestDaoImpl : QuestDao {
        override suspend fun getActiveTemplates(): List<QuestTemplateEntity> = withWriteLock {
            questTemplates.filter { it.active }
        }

        override suspend fun getTemplateByKey(key: String): QuestTemplateEntity? = withWriteLock {
            questTemplates.firstOrNull { it.key == key }
        }

        override suspend fun getTemplateById(id: Long): QuestTemplateEntity? = withWriteLock {
            questTemplates.firstOrNull { it.id == id }
        }

        override suspend fun countInstancesForTemplate(templateId: Long): Int = withWriteLock {
            questInstances.count { it.templateId == templateId }
        }

        override suspend fun getInstancesDependingOn(date: String, dependsOnKey: String): List<QuestInstanceEntity> =
            withWriteLock {
                val templateIds = questTemplates.filter { it.dependsOnTemplateKey == dependsOnKey }.map { it.id }.toSet()
                questInstances.filter { it.scheduledDate == date && it.templateId in templateIds }
            }

        override fun observeTemplates(): Flow<List<QuestTemplateEntity>> = templatesFlow

        override suspend fun upsertTemplates(templates: List<QuestTemplateEntity>) = withWriteLock {
            val map = questTemplates.associateBy { it.id }.toMutableMap()
            templates.forEach { template ->
                val withId = if (template.id == 0L) {
                    val id = nextId({ it.questTemplate }) { ids, next -> ids.copy(questTemplate = next) }
                    template.copy(id = id)
                } else {
                    template
                }
                map[withId.id] = withId
            }
            questTemplates = map.values.toMutableList()
            persistQuestTemplates()
            templatesFlow.value = questTemplates.toList()
        }

        override suspend fun upsertTemplate(template: QuestTemplateEntity): Long = withWriteLock {
            val withId = if (template.id == 0L) {
                val id = nextId({ it.questTemplate }) { ids, next -> ids.copy(questTemplate = next) }
                template.copy(id = id)
            } else {
                template
            }
            val idx = questTemplates.indexOfFirst { it.id == withId.id }
            if (idx >= 0) questTemplates[idx] = withId else questTemplates.add(withId)
            persistQuestTemplates()
            persistProgress()
            templatesFlow.value = questTemplates.toList()
            withId.id
        }

        override fun observeInstancesForDate(date: String): Flow<List<QuestInstanceEntity>> =
            instancesFlow.map { list -> list.filter { it.scheduledDate == date }.sortedBy { it.id } }

        override suspend fun getInstancesForDate(date: String): List<QuestInstanceEntity> = withWriteLock {
            questInstances.filter { it.scheduledDate == date }.sortedBy { it.id }
        }

        override suspend fun getInstance(id: Long): QuestInstanceEntity? = withWriteLock {
            questInstances.firstOrNull { it.id == id }
        }

        override suspend fun getWeeklyInstances(weekStart: String, weekEnd: String): List<QuestInstanceEntity> =
            withWriteLock {
                questInstances.filter {
                    it.type == "WEEKLY" && it.scheduledDate >= weekStart && it.scheduledDate <= weekEnd
                }
            }

        override suspend fun countCompletedInRange(weekStart: String, weekEnd: String): Int = withWriteLock {
            questInstances.count {
                it.scheduledDate >= weekStart && it.scheduledDate <= weekEnd && it.status == "COMPLETED"
            }
        }

        override suspend fun countTotalInRange(weekStart: String, weekEnd: String): Int = withWriteLock {
            questInstances.count { it.scheduledDate >= weekStart && it.scheduledDate <= weekEnd }
        }

        override suspend fun countIncompleteInRange(weekStart: String, weekEnd: String): Int = withWriteLock {
            questInstances.count {
                it.scheduledDate >= weekStart && it.scheduledDate <= weekEnd &&
                    it.status in listOf("AVAILABLE", "MISSED")
            }
        }

        override suspend fun insertInstance(instance: QuestInstanceEntity): Long = withWriteLock {
            val existing = questInstances.firstOrNull {
                it.templateId == instance.templateId && it.scheduledDate == instance.scheduledDate
            }
            if (existing != null) return@withWriteLock -1L
            val id = if (instance.id == 0L) {
                nextId({ it.questInstance }) { ids, next -> ids.copy(questInstance = next) }
            } else {
                instance.id
            }
            val saved = instance.copy(id = id)
            questInstances.add(saved)
            io.writeTask(id, gson.toJson(saved))
            persistProgress()
            instancesFlow.value = questInstances.sortedBy { it.id }
            id
        }

        override suspend fun updateInstance(instance: QuestInstanceEntity) = withWriteLock {
            val idx = questInstances.indexOfFirst { it.id == instance.id }
            if (idx >= 0) {
                questInstances[idx] = instance
                io.writeTask(instance.id, gson.toJson(instance))
                instancesFlow.value = questInstances.sortedBy { it.id }
            }
        }

        override suspend fun getAllInstances(): List<QuestInstanceEntity> = withWriteLock {
            questInstances.sortedBy { it.id }
        }

        override suspend fun deleteInstance(id: Long) = withWriteLock {
            val idx = questInstances.indexOfFirst { it.id == id }
            if (idx >= 0) {
                questInstances.removeAt(idx)
                io.deleteTask(id)
                persistProgress()
                instancesFlow.value = questInstances.sortedBy { it.id }
            }
        }

        override suspend fun countCompletedAll(): Int = withWriteLock {
            questInstances.count { it.status == "COMPLETED" }
        }

        override suspend fun getInstancesBeforeDate(date: String): List<QuestInstanceEntity> = withWriteLock {
            questInstances.filter { it.scheduledDate < date && it.status == "AVAILABLE" }
        }

        override fun observeInstancesByType(type: String): Flow<List<QuestInstanceEntity>> =
            instancesFlow.map { list -> list.filter { it.type == type }.sortedByDescending { it.scheduledDate } }

        override suspend fun getInstancesInRange(startDate: String, endDate: String): List<QuestInstanceEntity> =
            withWriteLock {
                questInstances.filter { it.scheduledDate >= startDate && it.scheduledDate <= endDate }
                    .sortedBy { it.scheduledDate }
            }

        override suspend fun updateTemplate(template: QuestTemplateEntity) = withWriteLock {
            val idx = questTemplates.indexOfFirst { it.id == template.id }
            if (idx >= 0) {
                questTemplates[idx] = template
                persistQuestTemplates()
                templatesFlow.value = questTemplates.toList()
            }
        }
    }

    private inner class XpDaoImpl : XpDao {
        override suspend fun insertLedger(entry: XpLedgerEntryEntity): Long = withWriteLock {
            if (xpLedger.any { it.sourceType == entry.sourceType && it.sourceId == entry.sourceId }) {
                throw SQLiteConstraintException("UNIQUE constraint failed: xp_ledger.sourceType, xp_ledger.sourceId")
            }
            val id = if (entry.id == 0L) {
                nextId({ it.xpLedger }) { ids, next -> ids.copy(xpLedger = next) }
            } else {
                entry.id
            }
            val saved = entry.copy(id = id)
            xpLedger.add(saved)
            persistXpLedger()
            persistProgress()
            ledgerFlow.value = xpLedger.sortedByDescending { it.createdAtEpochMs }
            id
        }

        override suspend fun findBySource(sourceType: String, sourceId: String): XpLedgerEntryEntity? =
            withWriteLock {
                xpLedger.firstOrNull { it.sourceType == sourceType && it.sourceId == sourceId }
            }

        override fun observeLedger(): Flow<List<XpLedgerEntryEntity>> = ledgerFlow

        override suspend fun getAllLedger(): List<XpLedgerEntryEntity> = withWriteLock {
            xpLedger.sortedBy { it.createdAtEpochMs }
        }

        override suspend fun sumXp(): Int = withWriteLock {
            xpLedger.sumOf { it.amount }
        }

        override suspend fun sumXpBetween(startMs: Long, endMs: Long): Int = withWriteLock {
            xpLedger.filter { it.createdAtEpochMs >= startMs && it.createdAtEpochMs < endMs }
                .sumOf { it.amount }
        }
    }

    private inner class ConfigDaoImpl : ConfigDao {
        override suspend fun get(key: String): UserConfigEntity? = withWriteLock {
            userJson.configs.firstOrNull { it.key == key }
        }

        override fun observe(key: String): Flow<UserConfigEntity?> = configFlow(key)

        override suspend fun upsert(config: UserConfigEntity) = withWriteLock {
            val list = userJson.configs.toMutableList()
            val idx = list.indexOfFirst { it.key == config.key }
            if (idx >= 0) list[idx] = config else list.add(config)
            userJson = userJson.copy(configs = list)
            persistUser()
            configFlow(config.key).value = config
        }
    }

    private inner class OutboxDaoImpl : OutboxDao {
        override suspend fun insert(entry: SyncOutboxEntity): Long = withWriteLock {
            val id = if (entry.id == 0L) {
                nextId({ it.syncOutbox }) { ids, next -> ids.copy(syncOutbox = next) }
            } else {
                entry.id
            }
            val saved = entry.copy(id = id)
            syncOutbox.add(saved)
            io.writeText(FILE_SYNC_OUTBOX, gson.toJson(syncOutbox))
            outboxFlows.forEach { (limit, flow) ->
                flow.value = syncOutbox.sortedByDescending { it.createdAtEpochMs }.take(limit)
            }
            id
        }

        override fun observeRecent(limit: Int): Flow<List<SyncOutboxEntity>> =
            outboxFlows.getOrPut(limit) {
                MutableStateFlow(syncOutbox.sortedByDescending { it.createdAtEpochMs }.take(limit))
            }

        override suspend fun markSynced(ids: List<Long>) = withWriteLock {
            val idSet = ids.toSet()
            syncOutbox = syncOutbox.map { if (it.id in idSet) it.copy(synced = true) else it }.toMutableList()
            io.writeText(FILE_SYNC_OUTBOX, gson.toJson(syncOutbox))
            outboxFlows.forEach { (limit, flow) ->
                flow.value = syncOutbox.sortedByDescending { it.createdAtEpochMs }.take(limit)
            }
        }

        override suspend fun getUnsynced(): List<SyncOutboxEntity> = withWriteLock {
            syncOutbox.filter { !it.synced }.sortedBy { it.createdAtEpochMs }
        }
    }

    private inner class AchievementDaoImpl : AchievementDao {
        override suspend fun upsertDefs(defs: List<AchievementDefEntity>) = withWriteLock {
            val map = achievementsJson.defs.associateBy { it.key }.toMutableMap()
            defs.forEach { map[it.key] = it }
            achievementsJson = achievementsJson.copy(defs = map.values.toList())
            persistAchievements()
            achievementDefsFlow.value = achievementsJson.defs
        }

        override fun observeDefs(): Flow<List<AchievementDefEntity>> = achievementDefsFlow

        override suspend fun getDefs(): List<AchievementDefEntity> = withWriteLock {
            achievementsJson.defs
        }

        override fun observeUnlocked(): Flow<List<PlayerAchievementEntity>> = unlockedFlow

        override suspend fun getUnlocked(): List<PlayerAchievementEntity> = withWriteLock {
            achievementsJson.unlocked
        }

        override suspend fun unlock(achievement: PlayerAchievementEntity) = withWriteLock {
            if (achievementsJson.unlocked.any { it.achievementKey == achievement.achievementKey }) return@withWriteLock
            achievementsJson = achievementsJson.copy(
                unlocked = achievementsJson.unlocked + achievement,
            )
            persistAchievements()
            unlockedFlow.value = achievementsJson.unlocked
        }
    }

    private inner class ModuleDaoImpl : ModuleDao {
        override fun observeBosses(): Flow<List<BossEntity>> = bossesFlow

        override suspend fun getActiveBoss(): BossEntity? = withWriteLock {
            bosses.firstOrNull { it.status == "ACTIVE" }
        }

        override suspend fun getBosses(): List<BossEntity> = withWriteLock {
            bosses.toList()
        }

        override suspend fun upsertBoss(boss: BossEntity): Long = withWriteLock {
            val withId = if (boss.id == 0L) {
                val id = nextId({ it.boss }) { ids, next -> ids.copy(boss = next) }
                boss.copy(id = id)
            } else {
                boss
            }
            val idx = bosses.indexOfFirst { it.id == withId.id }
            if (idx >= 0) bosses[idx] = withId else bosses.add(withId)
            io.writeText(FILE_BOSSES, gson.toJson(bosses))
            persistProgress()
            bossesFlow.value = bosses.sortedByDescending { it.id }
            withId.id
        }

        override suspend fun updateBoss(boss: BossEntity) = withWriteLock {
            val idx = bosses.indexOfFirst { it.id == boss.id }
            if (idx >= 0) {
                bosses[idx] = boss
                io.writeText(FILE_BOSSES, gson.toJson(bosses))
                bossesFlow.value = bosses.sortedByDescending { it.id }
            }
        }

        override fun observeSkills(): Flow<List<SkillEntity>> = skillsFlow

        override suspend fun upsertSkill(skill: SkillEntity): Long = withWriteLock {
            val withId = if (skill.id == 0L) {
                val id = nextId({ it.skill }) { ids, next -> ids.copy(skill = next) }
                skill.copy(id = id)
            } else {
                skill
            }
            val idx = skills.indexOfFirst { it.id == withId.id }
            if (idx >= 0) skills[idx] = withId else skills.add(withId)
            io.writeText(FILE_SKILLS, gson.toJson(skills))
            persistProgress()
            skillsFlow.value = skills.sortedWith(compareBy({ it.domain }, { it.name }))
            withId.id
        }

        override suspend fun updateSkill(skill: SkillEntity) = withWriteLock {
            val idx = skills.indexOfFirst { it.id == skill.id }
            if (idx >= 0) {
                skills[idx] = skill
                io.writeText(FILE_SKILLS, gson.toJson(skills))
                skillsFlow.value = skills.sortedWith(compareBy({ it.domain }, { it.name }))
            }
        }

        override fun observeDsa(): Flow<List<DsaProblemEntity>> = dsaFlow

        override suspend fun upsertDsa(problem: DsaProblemEntity): Long = withWriteLock {
            val withId = if (problem.id == 0L) {
                val id = nextId({ it.dsa }) { ids, next -> ids.copy(dsa = next) }
                problem.copy(id = id)
            } else {
                problem
            }
            val idx = dsa.indexOfFirst { it.id == withId.id }
            if (idx >= 0) dsa[idx] = withId else dsa.add(withId)
            io.writeText(FILE_DSA, gson.toJson(dsa))
            persistProgress()
            dsaFlow.value = dsa.sortedByDescending { it.id }
            withId.id
        }

        override suspend fun updateDsa(problem: DsaProblemEntity) = withWriteLock {
            val idx = dsa.indexOfFirst { it.id == problem.id }
            if (idx >= 0) {
                dsa[idx] = problem
                io.writeText(FILE_DSA, gson.toJson(dsa))
                dsaFlow.value = dsa.sortedByDescending { it.id }
            }
        }

        override fun observeWorkouts(): Flow<List<WorkoutLogEntity>> = workoutsFlow

        override fun observeWorkoutLog(date: String): Flow<WorkoutLogEntity?> =
            workoutLogFlows.getOrPut(date) { MutableStateFlow(workoutLogs[date]) }

        override suspend fun getWorkoutLog(date: String): WorkoutLogEntity? = withWriteLock {
            workoutLogs[date]
        }

        override suspend fun upsertWorkoutLog(log: WorkoutLogEntity): Long = withWriteLock {
            val id = if (log.id == 0L) {
                nextId({ it.workout }) { ids, next -> ids.copy(workout = next) }
            } else {
                log.id
            }
            val saved = log.copy(id = id, dayOfWeek = log.dayOfWeek.ifBlank { dayOfWeekForDate(log.date) })
            workoutLogs[saved.date] = saved
            persistWorkoutLogFile(saved)
            persistProgress()
            emitWorkoutFlows()
            id
        }

        override suspend fun getAllWorkoutLogs(): List<WorkoutLogEntity> = withWriteLock {
            workoutLogs.values.sortedByDescending { it.date }
        }

        override suspend fun deleteWorkoutLog(date: String) = withWriteLock {
            workoutLogs.remove(date)
            io.delete("${JsonFileIO.WORKOUTS_LOGS_DIR}/$date.json")
            emitWorkoutFlows()
        }

        override fun observeWorkoutRoutine(): Flow<WorkoutRoutineEntity> = workoutRoutineFlow

        override suspend fun getWorkoutRoutine(): WorkoutRoutineEntity = withWriteLock {
            workoutRoutine
        }

        override suspend fun upsertWorkoutRoutine(routine: WorkoutRoutineEntity) = withWriteLock {
            workoutRoutine = routine
            persistWorkoutRoutine()
            workoutRoutineFlow.value = workoutRoutine
        }

        override fun observeDietLog(date: String): Flow<DietLogEntity?> =
            dietLogFlows.getOrPut(date) { MutableStateFlow(dietLogs[date]) }

        override fun observeDietLogs(): Flow<List<DietLogEntity>> = dietLogsFlow

        override suspend fun getDietLog(date: String): DietLogEntity? = withWriteLock {
            dietLogs[date]
        }

        override suspend fun upsertDietLog(log: DietLogEntity) = withWriteLock {
            dietLogs[log.date] = log
            persistDietLogFile(log)
            emitDietFlows()
        }

        override suspend fun deleteDietLog(date: String) = withWriteLock {
            dietLogs.remove(date)
            io.delete("${JsonFileIO.DIET_LOGS_DIR}/$date.json")
            emitDietFlows()
        }

        override fun observeNutrition(date: String): Flow<NutritionLogEntity?> =
            nutritionFlows.getOrPut(date) {
                MutableStateFlow(nutritionFromDiet(date))
            }

        override suspend fun upsertNutrition(log: NutritionLogEntity) = withWriteLock {
            val foodId = nextId({ it.dietFood }) { ids, next -> ids.copy(dietFood = next) }
            val mealId = nextId({ it.dietMeal }) { ids, next -> ids.copy(dietMeal = next) }
            persistProgress()
            val food = FoodItemEntity(
                id = foodId,
                name = "Logged macros",
                calories = log.calories,
                protein = log.protein,
                carbs = log.carbs,
                fat = log.fat,
            )
            val meal = MealEntity(id = mealId, name = "Logged macros", foods = listOf(food))
            val diet = DietLogEntity(
                date = log.date,
                meals = listOf(meal),
                dailyTotals = NutritionTotalsEntity(log.calories, log.protein, log.carbs, log.fat),
            )
            dietLogs[log.date] = diet
            persistDietLogFile(diet)
            emitDietFlows()
        }

        override fun observeFocus(date: String): Flow<List<FocusSessionEntity>> =
            focusFlows.getOrPut(date) {
                MutableStateFlow(focus.filter { it.date == date }.sortedByDescending { it.id })
            }

        override suspend fun insertFocus(session: FocusSessionEntity): Long = withWriteLock {
            val id = if (session.id == 0L) {
                nextId({ it.focus }) { ids, next -> ids.copy(focus = next) }
            } else {
                session.id
            }
            val saved = session.copy(id = id)
            focus.add(saved)
            io.writeText(FILE_FOCUS, gson.toJson(focus))
            persistProgress()
            focusFlows.getOrPut(saved.date) {
                MutableStateFlow(emptyList())
            }.value = focus.filter { it.date == saved.date }.sortedByDescending { it.id }
            id
        }

        override fun observeJournal(date: String): Flow<JournalEntryEntity?> =
            journalFlows.getOrPut(date) {
                MutableStateFlow(journal.firstOrNull { it.date == date })
            }

        override suspend fun getJournal(date: String): JournalEntryEntity? = withWriteLock {
            journal.firstOrNull { it.date == date }
        }

        override suspend fun upsertJournal(entry: JournalEntryEntity) = withWriteLock {
            val idx = journal.indexOfFirst { it.date == entry.date }
            if (idx >= 0) journal[idx] = entry else journal.add(entry)
            io.writeText(FILE_JOURNAL, gson.toJson(journal))
            journalFlows.getOrPut(entry.date) { MutableStateFlow(entry) }.value = entry
        }

        override suspend fun insertMetric(metric: MetricLogEntity): Long = withWriteLock {
            val id = if (metric.id == 0L) {
                nextId({ it.metric }) { ids, next -> ids.copy(metric = next) }
            } else {
                metric.id
            }
            val saved = metric.copy(id = id)
            metrics.add(saved)
            io.writeText(FILE_METRICS, gson.toJson(metrics))
            persistProgress()
            recentMetricsFlow.value = metrics.sortedByDescending { it.recordedAtEpochMs }
            id
        }

        override suspend fun recentMetrics(type: String, limit: Int): List<MetricLogEntity> = withWriteLock {
            metrics.filter { it.metricType == type }
                .sortedByDescending { it.recordedAtEpochMs }
                .take(limit)
        }

        override fun observeRecentMetrics(limit: Int): Flow<List<MetricLogEntity>> =
            recentMetricsFlow.map { it.take(limit) }

        override suspend fun countDsaSolved(): Int = withWriteLock {
            dsa.count { it.status in listOf("SOLVED", "MASTERED") }
        }

        override suspend fun countBossCleared(): Int = withWriteLock {
            bosses.count { it.status == "CLEARED" }
        }

        override suspend fun getDsa(id: Long): DsaProblemEntity? = withWriteLock {
            dsa.firstOrNull { it.id == id }
        }

        override suspend fun findSkill(domain: String, name: String): SkillEntity? = withWriteLock {
            skills.firstOrNull { it.domain == domain && it.name == name }
        }

        override suspend fun upsertBossQuest(quest: BossQuestEntity): Long = withWriteLock {
            val existingIdx = bossQuests.indexOfFirst {
                it.bossId == quest.bossId && it.templateKey == quest.templateKey
            }
            val withId = if (quest.id == 0L && existingIdx < 0) {
                val id = nextId({ it.bossQuest }) { ids, next -> ids.copy(bossQuest = next) }
                quest.copy(id = id)
            } else if (existingIdx >= 0) {
                quest.copy(id = bossQuests[existingIdx].id)
            } else {
                quest
            }
            if (existingIdx >= 0) bossQuests[existingIdx] = withId else bossQuests.add(withId)
            io.writeText(FILE_BOSS_QUESTS, gson.toJson(bossQuests))
            persistProgress()
            bossQuestFlows.getOrPut(withId.bossId) {
                MutableStateFlow(emptyList())
            }.value = bossQuests.filter { it.bossId == withId.bossId }
            withId.id
        }

        override suspend fun getBossQuests(bossId: Long): List<BossQuestEntity> = withWriteLock {
            bossQuests.filter { it.bossId == bossId }
        }

        override suspend fun updateBossQuest(quest: BossQuestEntity) = withWriteLock {
            val idx = bossQuests.indexOfFirst { it.id == quest.id }
            if (idx >= 0) {
                bossQuests[idx] = quest
                io.writeText(FILE_BOSS_QUESTS, gson.toJson(bossQuests))
                bossQuestFlows.getOrPut(quest.bossId) {
                    MutableStateFlow(emptyList())
                }.value = bossQuests.filter { it.bossId == quest.bossId }
            }
        }

        override suspend fun upsertCareerNode(node: CareerNodeEntity): Long = withWriteLock {
            val withId = if (node.id == 0L) {
                val id = nextId({ it.careerNode }) { ids, next -> ids.copy(careerNode = next) }
                node.copy(id = id)
            } else {
                node
            }
            val idx = careerNodes.indexOfFirst { it.id == withId.id }
            if (idx >= 0) careerNodes[idx] = withId else careerNodes.add(withId)
            io.writeText(FILE_CAREER_NODES, gson.toJson(careerNodes))
            persistProgress()
            careerNodesFlow.value = careerNodes.sortedWith(compareBy({ it.track }, { it.orderIndex }))
            withId.id
        }

        override fun observeCareerNodes(): Flow<List<CareerNodeEntity>> = careerNodesFlow

        override suspend fun getCareerNodes(): List<CareerNodeEntity> = withWriteLock {
            careerNodes.sortedWith(compareBy({ it.track }, { it.orderIndex }))
        }

        override suspend fun getCareerNode(id: Long): CareerNodeEntity? = withWriteLock {
            careerNodes.firstOrNull { it.id == id }
        }

        override suspend fun upsertSeason(season: SeasonEntity): Long = withWriteLock {
            val withId = if (season.id == 0L) {
                val id = nextId({ it.season }) { ids, next -> ids.copy(season = next) }
                season.copy(id = id)
            } else {
                season
            }
            val idx = seasons.indexOfFirst { it.id == withId.id }
            if (idx >= 0) seasons[idx] = withId else seasons.add(withId)
            io.writeText(FILE_SEASONS, gson.toJson(seasons))
            persistProgress()
            activeSeasonFlow.value = seasons.firstOrNull { it.status == "ACTIVE" }
            withId.id
        }

        override suspend fun getActiveSeason(): SeasonEntity? = withWriteLock {
            seasons.firstOrNull { it.status == "ACTIVE" }
        }

        override fun observeActiveSeason(): Flow<SeasonEntity?> = activeSeasonFlow

        override suspend fun insertDismissedSuggestion(suggestion: DismissedSuggestionEntity): Long =
            withWriteLock {
                val id = if (suggestion.id == 0L) {
                    nextId({ it.dismissed }) { ids, next -> ids.copy(dismissed = next) }
                } else {
                    suggestion.id
                }
                val saved = suggestion.copy(id = id)
                dismissed.add(saved)
                io.writeText(FILE_DISMISSED, gson.toJson(dismissed))
                persistProgress()
                id
            }

        override suspend fun findDismissedSuggestion(key: String): DismissedSuggestionEntity? = withWriteLock {
            dismissed.firstOrNull { it.suggestionKey == key }
        }

        override suspend fun getDismissedSuggestionKeys(): List<String> = withWriteLock {
            dismissed.map { it.suggestionKey }
        }

        override suspend fun countWorkoutsInRange(startDate: String, endDate: String): Int = withWriteLock {
            workoutLogs.values.count {
                it.date >= startDate && it.date <= endDate && it.isTrainingDayComplete()
            }
        }

        override suspend fun countWorkoutDaysInRange(startDate: String, endDate: String): Int = withWriteLock {
            workoutLogs.values
                .filter { it.date >= startDate && it.date <= endDate && it.isTrainingDayComplete() }
                .map { it.date }
                .distinct()
                .size
        }

        override suspend fun countDsaSolvedInRange(startMs: Long, endMs: Long): Int = withWriteLock {
            dsa.count {
                it.status in listOf("SOLVED", "MASTERED") &&
                    it.solvedAtEpochMs != null &&
                    it.solvedAtEpochMs >= startMs &&
                    it.solvedAtEpochMs < endMs
            }
        }

        override suspend fun insertRoutineLog(log: RoutineLogEntity): Long = withWriteLock {
            val id = if (log.id == 0L) {
                nextId({ it.routine }) { ids, next -> ids.copy(routine = next) }
            } else {
                log.id
            }
            val saved = log.copy(id = id)
            routines.add(saved)
            io.writeText(FILE_ROUTINES, gson.toJson(routines))
            persistProgress()
            routineFlows.getOrPut(saved.date) {
                MutableStateFlow(emptyList())
            }.value = routines.filter { it.date == saved.date }.sortedBy { it.id }
            id
        }

        override suspend fun getRoutineLogsForDate(date: String): List<RoutineLogEntity> = withWriteLock {
            routines.filter { it.date == date }.sortedBy { it.id }
        }

        override fun observeRoutineLogs(date: String): Flow<List<RoutineLogEntity>> =
            routineFlows.getOrPut(date) {
                MutableStateFlow(routines.filter { it.date == date }.sortedBy { it.id })
            }

        override suspend fun sumMetricForDate(date: String, type: String): Float = withWriteLock {
            metrics.filter { it.date == date && it.metricType == type }.sumOf { it.value.toDouble() }.toFloat()
        }

        override suspend fun sumFocusMinutes(date: String): Int = withWriteLock {
            focus.filter { it.date == date }.sumOf { it.durationMinutes }
        }

        override suspend fun countDsaSolvedOnDate(dayStartMs: Long, dayEndMs: Long): Int = withWriteLock {
            dsa.count {
                it.status in listOf("SOLVED", "MASTERED") &&
                    it.solvedAtEpochMs != null &&
                    it.solvedAtEpochMs >= dayStartMs &&
                    it.solvedAtEpochMs < dayEndMs
            }
        }

        override suspend fun getNutrition(date: String): NutritionLogEntity? = withWriteLock {
            nutritionFromDiet(date)
        }

        override fun observeBossQuests(bossId: Long): Flow<List<BossQuestEntity>> =
            bossQuestFlows.getOrPut(bossId) {
                MutableStateFlow(bossQuests.filter { it.bossId == bossId })
            }

        override fun observeSystemDesignTopics(): Flow<List<SystemDesignTopicEntity>> = systemDesignTopicsFlow

        override suspend fun getSystemDesignTopics(): List<SystemDesignTopicEntity> = withWriteLock {
            systemDesignTopics.sortedBy { it.orderIndex }
        }

        override suspend fun upsertSystemDesignTopic(topic: SystemDesignTopicEntity) = withWriteLock {
            val idx = systemDesignTopics.indexOfFirst { it.id == topic.id }
            if (idx >= 0) systemDesignTopics[idx] = topic else systemDesignTopics.add(topic)
            io.writeText(FILE_SYSTEM_DESIGN_TOPICS, gson.toJson(systemDesignTopics))
            systemDesignTopicsFlow.value = systemDesignTopics.sortedBy { it.orderIndex }
        }

        override suspend fun replaceSystemDesignTopics(topics: List<SystemDesignTopicEntity>) = withWriteLock {
            systemDesignTopics = topics.toMutableList()
            io.writeText(FILE_SYSTEM_DESIGN_TOPICS, gson.toJson(systemDesignTopics))
            systemDesignTopicsFlow.value = systemDesignTopics.sortedBy { it.orderIndex }
        }

        override suspend fun getDsaProblems(): List<DsaProblemEntity> = withWriteLock {
            dsa.sortedByDescending { it.id }
        }
    }

    companion object {
        private const val FILE_USER = "user.json"
        private const val FILE_PROGRESS = "progress.json"
        private const val FILE_XP_LEDGER = "xp_ledger.json"
        private const val FILE_ACHIEVEMENTS = "achievements.json"
        private const val FILE_QUEST_TEMPLATES = "quest_templates.json"
        private const val FILE_SEASONS = "seasons.json"
        private const val FILE_SKILLS = "skills.json"
        private const val FILE_BOSSES = "bosses.json"
        private const val FILE_BOSS_QUESTS = "boss_quests.json"
        private const val FILE_CAREER_NODES = "career_nodes.json"
        private const val FILE_SYNC_OUTBOX = "sync_outbox.json"
        private const val FILE_DISMISSED = "dismissed.json"
        private const val FILE_WORKOUTS = "workouts.json"
        private const val FILE_WORKOUT_EXERCISES = "workout_exercises.json"
        private const val FILE_NUTRITION = "nutrition.json"
        private const val FILE_FOCUS = "focus.json"
        private const val FILE_JOURNAL = "journal.json"
        private const val FILE_METRICS = "metrics.json"
        private const val FILE_ROUTINES = "routines.json"
        private const val FILE_DSA = "dsa.json"
        private const val FILE_SYSTEM_DESIGN_TOPICS = "career/system-design/topics.json"
    }
}

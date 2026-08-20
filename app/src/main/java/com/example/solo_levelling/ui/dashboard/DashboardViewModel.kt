package com.example.solo_levelling.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.SeasonEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.domain.logic.MealCompletionPolicy
import com.example.solo_levelling.domain.logic.MealProgressState
import com.example.solo_levelling.domain.service.AdaptiveSuggestion
import com.example.solo_levelling.domain.service.CareerHubLogic
import com.example.solo_levelling.domain.service.NextUnlock
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleScope
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DashboardClockUi(
    val dateLabel: String = "",
    val hour: Int = 12,
)

data class ProgressSnapshot(
    val dsaPct: Int,
    val sdPct: Int,
    val workoutDoneToday: Boolean,
    val dietAdherencePct: Int,
    val streakCurrent: Int,
    val streakBest: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val refreshSuggestions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val profile: StateFlow<PlayerProfileEntity?> =
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val clockUi: StateFlow<DashboardClockUi> =
        profile.map { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val zoned = Instant.ofEpochMilli(container.clock.nowEpochMs()).atZone(zone)
            DashboardClockUi(
                dateLabel = zoned.toLocalDate().format(
                    DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.getDefault()),
                ),
                hour = zoned.hour,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardClockUi())

    val streak: StateFlow<StreakStateEntity?> =
        container.db.playerDao().observeStreak(SystemDefaults.PLAYER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val attributes: StateFlow<List<AttributeStatEntity>> =
        container.db.playerDao().observeAttributes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enabledModules: StateFlow<EnabledModules> =
        ModuleFlags.observeEnabledModules(
            container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
            container.db.configDao(),
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EnabledModules())

    private val homeQuestItems: StateFlow<List<HomeQuestItem>> =
        combine(
            profile.flatMapLatest { p ->
                val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                    .getOrDefault(ZoneId.systemDefault())
                val today = container.clock.today(zone).format(dateFmt)
                container.db.questDao().observeInstancesForDate(today)
            },
            enabledModules,
            container.db.questDao().observeTemplates(),
        ) { quests, modules, templates ->
            val tagsById = templates.associate { it.id to it.priorityTags }
            val keysById = templates.associate { it.id to it.key }
            HomeQuestPresentation.scopeForHome(
                quests = quests,
                tagsByTemplateId = tagsById,
                keysByTemplateId = keysById,
                allowsTemplate = { tags -> ModuleScope.allowsQuestTemplate(tags, modules) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Today's DAILY + RECOVERY quests for enabled modules (same scope as Home UI count). */
    val todayQuests: StateFlow<List<QuestInstanceEntity>> =
        homeQuestItems
            .map { items -> items.map { it.instance } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeQuestSections: StateFlow<HomeQuestSections> =
        homeQuestItems
            .map { HomeQuestPresentation.split(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                HomeQuestSections(emptyList(), emptyList(), emptyList(), false),
            )

    val activeBoss: StateFlow<BossEntity?> =
        container.db.moduleDao().observeBosses()
            .map { bosses -> bosses.firstOrNull { it.status == "ACTIVE" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeSeason: StateFlow<SeasonEntity?> =
        container.db.moduleDao().observeActiveSeason()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentAchievements: StateFlow<List<PlayerAchievementEntity>> =
        container.db.achievementDao().observeUnlocked()
            .map { list -> list.sortedByDescending { it.unlockedAtEpochMs }.take(3) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyCompletionPct: StateFlow<Float> =
        combine(profile, enabledModules, container.db.questDao().observeTemplates()) { p, modules, templates ->
            Triple(p, modules, templates)
        }.flatMapLatest { (p, modules, templates) ->
            flow {
                val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                    .getOrDefault(ZoneId.systemDefault())
                val today = container.clock.today(zone)
                val weekStart = today.with(java.time.DayOfWeek.MONDAY).format(dateFmt)
                val weekEnd = today.with(java.time.DayOfWeek.MONDAY).plusDays(6).format(dateFmt)
                val tagsById = templates.associate { it.id to it.priorityTags }
                val instances = container.db.questDao().getInstancesInRange(weekStart, weekEnd)
                    .filter { ModuleScope.allowsQuestTemplate(tagsById[it.templateId].orEmpty(), modules) }
                val total = instances.size
                val completed = instances.count { it.status == QuestStatus.COMPLETED.name }
                emit(if (total == 0) 0f else completed.toFloat() / total.toFloat())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val xpLast7Days: StateFlow<Int> =
        combine(profile, enabledModules) { p, modules -> p to modules }
            .flatMapLatest { (p, modules) ->
                flow {
                    val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                        .getOrDefault(ZoneId.systemDefault())
                    val today = container.clock.today(zone)
                    val startMs = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                    val endMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    emit(container.analytics.sumXpInRange(startMs, endMs, modules))
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val suggestions: StateFlow<List<AdaptiveSuggestion>> =
        refreshSuggestions.onStart { emit(Unit) }
            .flatMapLatest {
                flow { emit(container.adaptive.suggestions()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goalTitle: StateFlow<String?> =
        container.db.configDao().observe("goal_title")
            .map { it?.value?.takeIf { v -> v.isNotBlank() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val careerNextGoal: StateFlow<String> =
        combine(
            container.db.configDao().observe("career_next_goal"),
            container.db.configDao().observe("goal_title"),
        ) { next, title ->
            next?.value?.takeIf { it.isNotBlank() } ?: title?.value.orEmpty()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val heightCm: StateFlow<String> =
        container.db.configDao().observe("height_cm")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val weightKg: StateFlow<String> =
        container.db.configDao().observe("weight_kg")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val bmiEstimate: StateFlow<String> =
        container.db.configDao().observe("bmi_estimate")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val fitnessGoal: StateFlow<String> =
        container.db.configDao().observe("fitness_goal")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val workoutToday: StateFlow<WorkoutLogEntity?> =
        profile.flatMapLatest { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val today = container.clock.today(zone).format(dateFmt)
            container.db.moduleDao().observeWorkoutLog(today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val nutritionToday: StateFlow<NutritionLogEntity?> =
        profile.flatMapLatest { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val today = container.clock.today(zone).format(dateFmt)
            container.db.moduleDao().observeNutrition(today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dietLogToday: StateFlow<DietLogEntity?> =
        profile.flatMapLatest { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val today = container.clock.today(zone).format(dateFmt)
            container.db.moduleDao().observeDietLog(today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mealProgress: StateFlow<MealProgressState> =
        dietLogToday.map { log ->
            MealCompletionPolicy.mealProgressState(
                log = log,
                mealTotals = { meal -> container.modules.mealTotals(meal) },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MealCompletionPolicy.mealProgressState(null),
        )

    val calorieTarget: StateFlow<Int> =
        container.db.configDao().observe("calorie_target")
            .map { it?.value?.toIntOrNull() ?: 1800 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1800)

    val proteinTarget: StateFlow<Int> =
        container.db.configDao().observe("protein_target")
            .map { it?.value?.toIntOrNull() ?: 150 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 150)

    val workoutRoutine: StateFlow<WorkoutRoutineEntity> =
        container.db.moduleDao().observeWorkoutRoutine()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutRoutineEntity())

    val dsaPct: StateFlow<Int> =
        container.db.moduleDao().observeDsa()
            .map { CareerHubLogic.dsaOverallProgress(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val sdPct: StateFlow<Int> =
        container.db.moduleDao().observeSystemDesignTopics()
            .map { CareerHubLogic.sdTopicsProgress(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val backendPct: StateFlow<Int> =
        container.db.configDao().observe("backend_confidence")
            .map { CareerHubLogic.configInt(it?.value) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val behavioralPct: StateFlow<Int> =
        container.db.configDao().observe("behavioral_confidence")
            .map { CareerHubLogic.configInt(it?.value) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val mandatoryAreas: StateFlow<List<String>> =
        container.db.configDao().observe("career_mandatory_areas")
            .map { CareerHubLogic.parseCsv(it?.value) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workoutPlannedToday: StateFlow<Boolean> =
        combine(profile, workoutRoutine) { p, routine ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val dayKey = container.clock.today(zone).dayOfWeek.name.lowercase()
            routine.day(dayKey).enabled
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val workoutDoneToday: StateFlow<Boolean> =
        workoutToday.map { log ->
            log != null && log.isTrainingDayComplete()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val progressSnapshot: StateFlow<ProgressSnapshot> =
        combine(
            dsaPct,
            sdPct,
            workoutDoneToday,
            mealProgress,
            streak,
            enabledModules,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val dsa = values[0] as Int
            @Suppress("UNCHECKED_CAST")
            val sd = values[1] as Int
            @Suppress("UNCHECKED_CAST")
            val workoutDone = values[2] as Boolean
            @Suppress("UNCHECKED_CAST")
            val meals = values[3] as MealProgressState
            @Suppress("UNCHECKED_CAST")
            val streakState = values[4] as StreakStateEntity?
            @Suppress("UNCHECKED_CAST")
            val modules = values[5] as EnabledModules

            val dietPct = if (!modules.diet || meals.requiredCount <= 0) {
                0
            } else {
                (meals.loggedCount.toFloat() / meals.requiredCount * 100f).toInt().coerceAtMost(100)
            }

            ProgressSnapshot(
                dsaPct = dsa,
                sdPct = sd,
                workoutDoneToday = workoutDone,
                dietAdherencePct = dietPct,
                streakCurrent = streakState?.current ?: 0,
                streakBest = streakState?.best ?: 0,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProgressSnapshot(0, 0, false, 0, 0, 0),
        )

    val nextUnlock: StateFlow<NextUnlock?> =
        profile.flatMapLatest {
            flow { emit(runCatching { container.analytics.nextUnlock() }.getOrNull()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
            if (p?.onboardingDone == true) {
                container.questGeneration.generateForToday(p.timezone)
            }
        }
    }

    fun dismissSuggestion(key: String) {
        viewModelScope.launch {
            container.adaptive.dismissSuggestion(key)
            refreshSuggestions.emit(Unit)
        }
    }

    suspend fun complete(instanceId: Long): QuestCompletionService.Result =
        container.questCompletion.complete(instanceId)

    suspend fun undo(instanceId: Long): Boolean =
        container.questCompletion.undo(instanceId)

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(container) as T
        }
    }
}

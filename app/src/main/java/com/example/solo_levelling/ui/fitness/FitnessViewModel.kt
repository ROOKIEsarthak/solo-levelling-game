package com.example.solo_levelling.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlannedExerciseEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.logic.ActivityDatePolicy
import com.example.solo_levelling.domain.logic.DayRelation
import com.example.solo_levelling.domain.logic.MealCompletionPolicy
import com.example.solo_levelling.domain.logic.MealProgressState
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.NutritionFeedbackService
import com.example.solo_levelling.domain.service.NutritionTargets
import com.example.solo_levelling.domain.service.PostMealFeedback
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val fallbackToday = container.clock.today(ZoneId.systemDefault()).format(dateFmt)

    private val today = container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .map { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            container.clock.today(zone).format(dateFmt)
        }

    private val _selectedWorkoutDate = MutableStateFlow<String?>(null)
    val selectedWorkoutDate: StateFlow<String?> = _selectedWorkoutDate.asStateFlow()

    private val _selectedDietDate = MutableStateFlow<String?>(null)
    val selectedDietDate: StateFlow<String?> = _selectedDietDate.asStateFlow()

    private val _postMealFeedback = MutableStateFlow<PostMealFeedback?>(null)
    val postMealFeedback: StateFlow<PostMealFeedback?> = _postMealFeedback.asStateFlow()

    val todayDate: StateFlow<String> =
        today.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), fallbackToday)

    val enabledModules: StateFlow<EnabledModules> =
        ModuleFlags.observeEnabledModules(
            container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
            container.db.configDao(),
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EnabledModules())

    fun selectWorkoutDate(date: String) {
        if (!isSelectable(date)) return
        _selectedWorkoutDate.value = date
    }

    fun resetWorkoutDateToToday() {
        _selectedWorkoutDate.value = null
    }

    fun selectDietDate(date: String) {
        if (!isSelectable(date)) return
        _selectedDietDate.value = date
    }

    fun resetDietDateToToday() {
        _selectedDietDate.value = null
    }

    fun dismissPostMealFeedback() {
        _postMealFeedback.value = null
    }

    private val _reopenMealId = MutableStateFlow<Long?>(null)
    val reopenMealId: StateFlow<Long?> = _reopenMealId.asStateFlow()

    fun requestReopenMeal(mealId: Long) {
        _reopenMealId.value = mealId
    }

    fun consumeReopenMeal() {
        _reopenMealId.value = null
    }

    fun onFoodSaved(mealId: Long, mealName: String, food: FoodItemEntity, dietLog: DietLogEntity?) {
        _postMealFeedback.value = buildPostMealFeedback(mealId, mealName, food, dietLog)
    }

    fun buildPostMealFeedback(
        mealId: Long,
        mealName: String,
        food: FoodItemEntity,
        dietLog: DietLogEntity?,
    ): PostMealFeedback {
        val mealProgress = MealCompletionPolicy.mealProgressState(
            log = dietLog,
            mealTotals = { meal -> container.modules.mealTotals(meal) },
        )
        val targets = nutritionTargets()
        val workoutDone = workoutLogToday.value?.exercises?.any { it.sets.isNotEmpty() } == true
        val modules = enabledModules.value
        return NutritionFeedbackService.buildPostMealFeedback(
            mealId = mealId,
            mealName = mealName,
            food = food,
            dailyTotals = dietLog?.dailyTotals
                ?: com.example.solo_levelling.data.db.entity.NutritionTotalsEntity(),
            targets = targets,
            fitnessGoal = fitnessGoal.value,
            mealProgress = mealProgress,
            workoutDoneToday = workoutDone,
            dietAndWorkoutEnabled = modules.diet && modules.workout,
        )
    }

    fun canWrite(date: String): Boolean {
        val today = parseDate(todayDate.value) ?: return false
        val selected = parseDate(date) ?: return false
        return ActivityDatePolicy.canWriteRecord(today, selected)
    }

    fun canGoNext(date: String): Boolean {
        val today = parseDate(todayDate.value) ?: return false
        val selected = parseDate(date) ?: return false
        return ActivityDatePolicy.canGoToNextDay(today, selected)
    }

    fun dateGuidance(date: String): String {
        val today = parseDate(todayDate.value) ?: return SystemMessages.DATE_TODAY_ACTION
        val selected = parseDate(date) ?: return SystemMessages.DATE_TODAY_ACTION
        return SystemMessages.dateGuidance(ActivityDatePolicy.relation(today, selected))
    }

    private fun isSelectable(date: String): Boolean {
        val today = parseDate(todayDate.value) ?: return false
        val selected = parseDate(date) ?: return false
        return ActivityDatePolicy.relation(today, selected) != DayRelation.Future
    }

    private fun parseDate(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw) }.getOrNull()

    val workouts: StateFlow<List<WorkoutLogEntity>> =
        container.db.moduleDao().observeWorkouts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workoutRoutine: StateFlow<WorkoutRoutineEntity> =
        container.db.moduleDao().observeWorkoutRoutine()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutRoutineEntity())

    val nutritionToday: StateFlow<NutritionLogEntity?> = today
        .flatMapLatest { date -> container.db.moduleDao().observeNutrition(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val workoutLogToday: StateFlow<WorkoutLogEntity?> = today
        .flatMapLatest { date -> container.db.moduleDao().observeWorkoutLog(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dietLogs: StateFlow<List<DietLogEntity>> =
        container.db.moduleDao().observeDietLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedWorkoutLog: StateFlow<WorkoutLogEntity?> =
        today.flatMapLatest { todayStr ->
            val dateFlow = _selectedWorkoutDate.map { it ?: todayStr }
            dateFlow.flatMapLatest { date -> container.db.moduleDao().observeWorkoutLog(date) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedDietLog: StateFlow<DietLogEntity?> =
        today.flatMapLatest { todayStr ->
            val dateFlow = _selectedDietDate.map { it ?: todayStr }
            dateFlow.flatMapLatest { date -> container.db.moduleDao().observeDietLog(date) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mealProgress: StateFlow<MealProgressState> =
        selectedDietLog.map { log ->
            MealCompletionPolicy.mealProgressState(
                log = log,
                mealTotals = { meal -> container.modules.mealTotals(meal) },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MealCompletionPolicy.mealProgressState(null),
        )

    val targetsConfigured: StateFlow<Boolean> =
        container.db.configDao().observe("calorie_target")
            .map { it?.value?.toIntOrNull()?.let { v -> v > 0 } == true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    val proteinTarget: StateFlow<Int> =
        container.db.configDao().observe("protein_target")
            .map { it?.value?.toIntOrNull() ?: 150 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 150)

    val carbTarget: StateFlow<Int> =
        container.db.configDao().observe("carb_target")
            .map { it?.value?.toIntOrNull() ?: 200 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 200)

    val fatTarget: StateFlow<Int> =
        container.db.configDao().observe("fat_target")
            .map { it?.value?.toIntOrNull() ?: 60 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 60)

    val calorieTarget: StateFlow<Int> =
        container.db.configDao().observe("calorie_target")
            .map { it?.value?.toIntOrNull() ?: 1800 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1800)

    val workoutSplitId: StateFlow<String?> =
        container.db.configDao().observe("workout_split_id")
            .map<UserConfigEntity?, String?> { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun nutritionTargets(): NutritionTargets {
        val configured = targetsConfigured.value
        return NutritionTargets(
            calorieTarget = if (configured) calorieTarget.value else 0,
            proteinTarget = if (configured) proteinTarget.value else 0,
            carbTarget = if (configured) carbTarget.value else 0,
            fatTarget = if (configured) fatTarget.value else 0,
            targetsConfigured = configured,
        )
    }

    suspend fun applyWorkoutSplit(
        splitId: String,
        dayMapCsv: String,
        confirmEarlyChange: Boolean,
    ): String? = container.modules.applyWorkoutSplit(splitId, dayMapCsv, confirmEarlyChange)

    suspend fun saveRoutineDay(dayKey: String, plan: WorkoutDayPlanEntity) {
        container.modules.saveRoutineDay(dayKey, plan)
    }

    suspend fun setRestDay(dayKey: String) {
        container.modules.setRestDay(dayKey)
    }

    suspend fun upsertPlannedExercise(
        dayKey: String,
        exercise: PlannedExerciseEntity,
        workoutName: String,
    ) {
        container.modules.upsertPlannedExercise(dayKey, exercise, workoutName)
    }

    suspend fun removePlannedExercise(dayKey: String, exerciseId: Long) {
        container.modules.removePlannedExercise(dayKey, exerciseId)
    }

    suspend fun reorderPlannedExercise(dayKey: String, exerciseId: Long, moveUp: Boolean) {
        container.modules.reorderPlannedExercise(dayKey, exerciseId, moveUp)
    }

    suspend fun startOrGetWorkoutLog(date: String): WorkoutLogEntity =
        container.modules.startOrGetWorkoutLog(date)

    suspend fun upsertWorkoutLog(log: WorkoutLogEntity): Long =
        container.modules.upsertWorkoutLog(log)

    suspend fun completeRestDay(date: String, activeRest: Boolean): WorkoutLogEntity =
        container.modules.completeRestDay(date, activeRest)

    suspend fun deleteWorkoutLog(date: String) {
        container.modules.deleteWorkoutLog(date)
    }

    suspend fun removeExerciseFromLog(date: String, exerciseId: Long) {
        container.modules.removeExerciseFromLog(date, exerciseId)
    }

    suspend fun upsertLoggedExercise(date: String, exercise: LoggedExerciseEntity) {
        container.modules.upsertLoggedExercise(date, exercise)
    }

    suspend fun addMeal(date: String, name: String): Long =
        container.modules.addMeal(date, name)

    suspend fun deleteMeal(date: String, mealId: Long) {
        container.modules.deleteMeal(date, mealId)
    }

    suspend fun upsertFood(date: String, mealId: Long, food: FoodItemEntity) {
        container.modules.upsertFood(date, mealId, food)
    }

    suspend fun getDietLog(date: String): DietLogEntity? =
        container.modules.getDietLog(date)

    suspend fun deleteFood(date: String, mealId: Long, foodId: Long) {
        container.modules.deleteFood(date, mealId, foodId)
    }

    suspend fun repeatMeal(date: String, destName: String, foods: List<FoodItemEntity>) {
        val id = container.modules.addMeal(date, destName)
        foods.forEach { food ->
            container.modules.upsertFood(date, id, food.copy(id = 0))
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FitnessViewModel(container) as T
        }
    }
}

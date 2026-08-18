package com.example.solo_levelling.ui.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
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
    container: AppContainer,
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

    val todayDate: StateFlow<String> =
        today.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), fallbackToday)

    fun selectWorkoutDate(date: String) {
        _selectedWorkoutDate.value = date
    }

    fun resetWorkoutDateToToday() {
        _selectedWorkoutDate.value = null
    }

    fun selectDietDate(date: String) {
        _selectedDietDate.value = date
    }

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

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FitnessViewModel(container) as T
        }
    }
}

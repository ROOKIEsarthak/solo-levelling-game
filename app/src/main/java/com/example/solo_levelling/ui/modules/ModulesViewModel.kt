package com.example.solo_levelling.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.RoutineLogEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ModulesViewModel(
    container: AppContainer,
) : ViewModel() {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val today = container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .map { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            container.clock.today(zone).format(dateFmt)
        }

    val dsa: StateFlow<List<DsaProblemEntity>> =
        container.db.moduleDao().observeDsa()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val workouts: StateFlow<List<WorkoutEntity>> =
        container.db.moduleDao().observeWorkouts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bosses: StateFlow<List<BossEntity>> =
        container.db.moduleDao().observeBosses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val skills: StateFlow<List<SkillEntity>> =
        container.db.moduleDao().observeSkills()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val careerNodes: StateFlow<List<CareerNodeEntity>> =
        container.db.moduleDao().observeCareerNodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nutritionToday: StateFlow<NutritionLogEntity?> = today
        .flatMapLatest { date -> container.db.moduleDao().observeNutrition(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val journalToday: StateFlow<JournalEntryEntity?> = today
        .flatMapLatest { date -> container.db.moduleDao().observeJournal(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val focusToday: StateFlow<List<FocusSessionEntity>> = today
        .flatMapLatest { date -> container.db.moduleDao().observeFocus(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routinesToday: StateFlow<List<RoutineLogEntity>> = today
        .flatMapLatest { date -> container.db.moduleDao().observeRoutineLogs(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentMetrics: StateFlow<List<MetricLogEntity>> =
        container.db.moduleDao().observeRecentMetrics(20)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ModulesViewModel(container) as T
        }
    }
}

package com.example.solo_levelling.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.RoutineLogEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.domain.port.MetricIngestPort
import com.example.solo_levelling.domain.service.ModuleService
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
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val modules: ModuleService,
    private val metricIngest: MetricIngestPort,
) : ViewModel() {
    constructor(container: AppContainer) : this(
        container.db,
        container.clock,
        container.modules,
        container.metricIngest,
    )

    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val today = db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .map { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            clock.today(zone).format(dateFmt)
        }

    val dsa: StateFlow<List<DsaProblemEntity>> =
        db.moduleDao().observeDsa()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bosses: StateFlow<List<BossEntity>> =
        db.moduleDao().observeBosses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val skills: StateFlow<List<SkillEntity>> =
        db.moduleDao().observeSkills()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val careerNodes: StateFlow<List<CareerNodeEntity>> =
        db.moduleDao().observeCareerNodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val journalToday: StateFlow<JournalEntryEntity?> = today
        .flatMapLatest { date -> db.moduleDao().observeJournal(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val focusToday: StateFlow<List<FocusSessionEntity>> = today
        .flatMapLatest { date -> db.moduleDao().observeFocus(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routinesToday: StateFlow<List<RoutineLogEntity>> = today
        .flatMapLatest { date -> db.moduleDao().observeRoutineLogs(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentMetrics: StateFlow<List<MetricLogEntity>> =
        db.moduleDao().observeRecentMetrics(20)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun logFocus(durationMinutes: Int, label: String) {
        modules.logFocus(durationMinutes, label)
    }

    suspend fun saveJournal(content: String) {
        modules.saveJournal(content)
    }

    suspend fun createBoss(title: String, description: String, xpReward: Int) {
        modules.createBoss(title, description, xpReward)
    }

    suspend fun logRoutine(kind: String) {
        modules.logRoutine(kind)
    }

    suspend fun advanceCareerNode(id: Long) {
        modules.advanceCareerNode(id)
    }

    suspend fun addDsaProblem(title: String, difficulty: String, topic: String) {
        modules.addDsaProblem(title, difficulty, topic)
    }

    suspend fun markAttempted(id: Long) {
        modules.markAttempted(id)
    }

    suspend fun solveDsa(id: Long) {
        modules.solveDsa(id)
    }

    suspend fun masterDsa(id: Long) {
        modules.masterDsa(id)
    }

    suspend fun ingestMetric(type: String, value: Float) {
        metricIngest.ingest(type, value)
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ModulesViewModel(container) as T
        }
    }
}

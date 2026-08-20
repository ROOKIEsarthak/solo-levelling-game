package com.example.solo_levelling.ui.career

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.domain.service.ModuleService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CareerViewModel(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val modules: ModuleService,
) : ViewModel() {
    constructor(container: AppContainer) : this(
        container.db,
        container.clock,
        container.modules,
    )

    val dsa: StateFlow<List<DsaProblemEntity>> =
        db.moduleDao().observeDsa()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val systemDesignTopics: StateFlow<List<SystemDesignTopicEntity>> =
        db.moduleDao().observeSystemDesignTopics()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val careerNodes: StateFlow<List<CareerNodeEntity>> =
        db.moduleDao().observeCareerNodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextGoal: StateFlow<String> =
        db.configDao().observe("career_next_goal")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val goalReason: StateFlow<String> =
        db.configDao().observe("career_goal_reason")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val currentRole: StateFlow<String> =
        db.configDao().observe("career_current_role")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val targetRole: StateFlow<String> =
        db.configDao().observe("career_target_role")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val mandatoryAreasCsv: StateFlow<String> =
        db.configDao().observe("career_mandatory_areas")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val backendConfidence: StateFlow<String?> =
        db.configDao().observe("backend_confidence")
            .map { it?.value }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val behavioralConfidence: StateFlow<String?> =
        db.configDao().observe("behavioral_confidence")
            .map { it?.value }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun nowEpochMs(): Long = clock.nowEpochMs()

    suspend fun markAttempted(id: Long) {
        modules.markAttempted(id)
    }

    suspend fun solveDsa(id: Long) {
        modules.solveDsa(id)
    }

    suspend fun masterDsa(id: Long) {
        modules.masterDsa(id)
    }

    suspend fun addDsaProblem(title: String, difficulty: String, topic: String) {
        modules.addDsaProblem(title, difficulty, topic)
    }

    suspend fun markSystemDesignConcept(topicId: String, conceptId: String, status: String) {
        modules.markSystemDesignConcept(topicId, conceptId, status)
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CareerViewModel(container) as T
        }
    }
}

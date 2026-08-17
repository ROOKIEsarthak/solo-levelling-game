package com.example.solo_levelling.ui.career

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CareerViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val dsa: StateFlow<List<DsaProblemEntity>> =
        container.db.moduleDao().observeDsa()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val systemDesignTopics: StateFlow<List<SystemDesignTopicEntity>> =
        container.db.moduleDao().observeSystemDesignTopics()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val careerNodes: StateFlow<List<CareerNodeEntity>> =
        container.db.moduleDao().observeCareerNodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextGoal: StateFlow<String> =
        container.db.configDao().observe("career_next_goal")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val goalReason: StateFlow<String> =
        container.db.configDao().observe("career_goal_reason")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val currentRole: StateFlow<String> =
        container.db.configDao().observe("career_current_role")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val targetRole: StateFlow<String> =
        container.db.configDao().observe("career_target_role")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val mandatoryAreasCsv: StateFlow<String> =
        container.db.configDao().observe("career_mandatory_areas")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val backendConfidence: StateFlow<String?> =
        container.db.configDao().observe("backend_confidence")
            .map { it?.value }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val behavioralConfidence: StateFlow<String?> =
        container.db.configDao().observe("behavioral_confidence")
            .map { it?.value }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun nowEpochMs(): Long = container.clock.nowEpochMs()

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CareerViewModel(container) as T
        }
    }
}

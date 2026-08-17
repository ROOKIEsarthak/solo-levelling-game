package com.example.solo_levelling.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CharacterViewModel(
    container: AppContainer,
) : ViewModel() {
    val profile: StateFlow<PlayerProfileEntity?> =
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val attributes: StateFlow<List<AttributeStatEntity>> =
        container.db.playerDao().observeAttributes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val streak: StateFlow<StreakStateEntity?> =
        container.db.playerDao().observeStreak(SystemDefaults.PLAYER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ledgerHistory: StateFlow<List<XpLedgerEntryEntity>> =
        container.db.xpDao().observeLedger()
            .map { entries -> entries.take(50) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentRole: StateFlow<String> =
        container.db.configDao().observe("career_current_role")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val careerYears: StateFlow<String> =
        container.db.configDao().observe("career_years_experience")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val targetRole: StateFlow<String> =
        container.db.configDao().observe("career_target_role")
            .map { it?.value.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

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

    val enabledModules: StateFlow<EnabledModules> =
        ModuleFlags.observeEnabledModules(
            container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
            container.db.configDao(),
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EnabledModules())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CharacterViewModel(container) as T
        }
    }
}

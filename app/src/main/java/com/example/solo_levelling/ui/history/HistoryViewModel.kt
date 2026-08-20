package com.example.solo_levelling.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.core.config.SystemDefaults
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    container: AppContainer,
) : ViewModel() {
    private val reader = container.activeProgression

    val recentXp: StateFlow<List<XpLedgerEntryEntity>> =
        reader.observeActiveLedger(limit = 20)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enabledModules: StateFlow<EnabledModules> =
        ModuleFlags.observeEnabledModules(
            container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
            container.db.configDao(),
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EnabledModules())

    val recentWorkouts: StateFlow<List<WorkoutLogEntity>> =
        combine(
            container.db.moduleDao().observeWorkouts(),
            enabledModules,
        ) { logs, modules ->
            if (!modules.workout) emptyList()
            else logs.sortedByDescending { it.date }.take(10)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(container) as T
        }
    }
}

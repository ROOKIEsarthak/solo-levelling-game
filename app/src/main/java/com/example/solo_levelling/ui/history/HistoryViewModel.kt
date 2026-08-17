package com.example.solo_levelling.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(
    container: AppContainer,
) : ViewModel() {
    val recentXp: StateFlow<List<XpLedgerEntryEntity>> =
        container.db.xpDao().observeLedger()
            .map { entries -> entries.sortedByDescending { it.createdAtEpochMs }.take(20) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentWorkouts: StateFlow<List<WorkoutLogEntity>> =
        container.db.moduleDao().observeWorkouts()
            .map { logs -> logs.sortedByDescending { it.date }.take(10) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HistoryViewModel(container) as T
        }
    }
}

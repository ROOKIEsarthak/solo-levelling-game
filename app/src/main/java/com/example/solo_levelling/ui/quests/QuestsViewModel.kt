package com.example.solo_levelling.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class QuestsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    val quests: StateFlow<List<QuestInstanceEntity>> =
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
            .flatMapLatest { p ->
                val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                    .getOrDefault(ZoneId.systemDefault())
                val today = container.clock.today(zone).format(dateFmt)
                container.db.questDao().observeInstancesForDate(today)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuestsViewModel(container) as T
        }
    }
}

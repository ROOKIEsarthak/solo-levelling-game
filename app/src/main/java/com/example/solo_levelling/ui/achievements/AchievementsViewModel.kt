package com.example.solo_levelling.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AchievementsViewModel(
    container: AppContainer,
) : ViewModel() {
    val defs: StateFlow<List<AchievementDefEntity>> =
        combine(
            container.db.achievementDao().observeDefs(),
            ModuleFlags.observeEnabledModules(
                container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
                container.db.configDao(),
            ),
        ) { defs, modules ->
            visibleAchievementDefs(defs, modules)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unlocked: StateFlow<List<PlayerAchievementEntity>> =
        container.db.achievementDao().observeUnlocked()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AchievementsViewModel(container) as T
        }
    }
}

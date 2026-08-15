package com.example.solo_levelling.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CharacterViewModel(container) as T
        }
    }
}

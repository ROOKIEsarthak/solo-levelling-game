package com.example.solo_levelling.ui.levelup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.event.DomainEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LevelUpEvent {
    data class LevelUp(val oldLevel: Int, val newLevel: Int) : LevelUpEvent()
    data class RankUp(val oldRank: String, val newRank: String) : LevelUpEvent()
}

class LevelUpViewModel(
    container: AppContainer,
) : ViewModel() {
    private val _pendingEvent = MutableStateFlow<LevelUpEvent?>(null)
    val pendingEvent: StateFlow<LevelUpEvent?> = _pendingEvent.asStateFlow()

    init {
        viewModelScope.launch {
            container.eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.LevelUp ->
                        _pendingEvent.value = LevelUpEvent.LevelUp(event.oldLevel, event.newLevel)
                    is DomainEvent.RankUp ->
                        _pendingEvent.value = LevelUpEvent.RankUp(event.oldRank, event.newRank)
                    else -> Unit
                }
            }
        }
    }

    fun dismiss() {
        _pendingEvent.value = null
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LevelUpViewModel(container) as T
        }
    }
}

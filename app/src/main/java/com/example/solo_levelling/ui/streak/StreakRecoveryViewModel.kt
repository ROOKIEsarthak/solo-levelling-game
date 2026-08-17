package com.example.solo_levelling.ui.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StreakBrokenEvent(val previousStreak: Int, val best: Int)

class StreakRecoveryViewModel(
    eventBus: EventBus,
) : ViewModel() {
    private val _pending = MutableStateFlow<StreakBrokenEvent?>(null)
    val pending: StateFlow<StreakBrokenEvent?> = _pending.asStateFlow()

    init {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.StreakBroken ->
                        _pending.value = StreakBrokenEvent(event.previousStreak, event.best)
                    else -> Unit
                }
            }
        }
    }

    fun dismiss() {
        _pending.value = null
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StreakRecoveryViewModel(container.eventBus) as T
        }
    }
}

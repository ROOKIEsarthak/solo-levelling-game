package com.example.solo_levelling.ui.levelup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.service.AnalyticsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LevelUpEvent {
    data class LevelUp(val oldLevel: Int, val newLevel: Int) : LevelUpEvent()
    data class RankUp(val oldRank: String, val newRank: String) : LevelUpEvent()
}

class LevelUpViewModel(
    private val eventBus: EventBus,
    private val analytics: AnalyticsService? = null,
) : ViewModel() {
    private val _pendingEvent = MutableStateFlow<LevelUpEvent?>(null)
    val pendingEvent: StateFlow<LevelUpEvent?> = _pendingEvent.asStateFlow()

    private val _improvementPercent = MutableStateFlow<Float?>(null)
    val improvementPercent: StateFlow<Float?> = _improvementPercent.asStateFlow()

    private val _motivationalMessage = MutableStateFlow("")
    val motivationalMessage: StateFlow<String> = _motivationalMessage.asStateFlow()

    init {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.LevelUp -> {
                        val snap = analytics?.let { runCatching { it.improvementSnapshot() }.getOrNull() }
                        val pct = snap?.improvementPercent
                        _improvementPercent.value = pct
                        val intensity = SystemMessages.intensityForImprovement(pct)
                        _motivationalMessage.value =
                            SystemMessages.pickLevelUp(intensity, event.newLevel)
                        _pendingEvent.value = LevelUpEvent.LevelUp(event.oldLevel, event.newLevel)
                    }
                    is DomainEvent.RankUp ->
                        _pendingEvent.value = LevelUpEvent.RankUp(event.oldRank, event.newRank)
                    is DomainEvent.XpReversed,
                    is DomainEvent.QuestUndone,
                    -> _pendingEvent.value = null
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
                LevelUpViewModel(container.eventBus, container.analytics) as T
        }
    }
}

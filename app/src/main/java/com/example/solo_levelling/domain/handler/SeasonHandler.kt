package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.domain.service.SeasonService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SeasonHandler(
    private val eventBus: EventBus,
    private val seasonService: SeasonService,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                if (event is DomainEvent.XpAwarded) {
                    seasonService.addSeasonXp(event.amount)
                }
            }
        }
    }
}

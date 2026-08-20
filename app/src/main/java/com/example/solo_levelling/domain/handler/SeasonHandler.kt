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
        // Season XP for quest complete/undo is applied by PostQuestCompletionCoordinator.
        // Other XP awards (module, achievement, boss) still flow through EventBus.
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.XpAwarded -> {
                        if (event.sourceType.equals("QUEST_INSTANCE", ignoreCase = true)) return@collect
                        if (event.sourceType.equals("QUEST_UNDO_PENALTY", ignoreCase = true)) {
                            seasonService.addSeasonXp(event.amount)
                            return@collect
                        }
                        seasonService.addSeasonXp(event.amount)
                    }
                    is DomainEvent.XpReversed -> {
                        if (event.sourceType.equals("QUEST_UNDO", ignoreCase = true)) return@collect
                        seasonService.addSeasonXp(event.amount)
                    }
                    else -> Unit
                }
            }
        }
    }
}

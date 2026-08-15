package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.SyncOutboxEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SyncOutboxHandler(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                db.outboxDao().insert(
                    SyncOutboxEntity(
                        eventType = event::class.simpleName ?: "Unknown",
                        payloadJson = event.toString(),
                        createdAtEpochMs = clock.nowEpochMs(),
                    ),
                )
            }
        }
    }
}

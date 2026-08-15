package com.example.solo_levelling.core.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventBusTest {
    @Test
    fun p_publish_deliversEventToCollectors() = runTest {
        val bus = EventBus()
        val received = mutableListOf<DomainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.events.collect { received += it }
        }
        bus.publish(DomainEvent.LevelUp(1, 2))
        assertEquals(1, received.size)
        assertTrue(received[0] is DomainEvent.LevelUp)
    }

    @Test
    fun e_tryPublish_succeedsWithBuffer() {
        val bus = EventBus()
        assertTrue(bus.tryPublish(DomainEvent.RankUp("E", "D")))
    }

    @Test
    fun p_publish_deliversMultipleEventsInOrder() = runTest {
        val bus = EventBus()
        val received = mutableListOf<DomainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.events.collect { received += it }
        }
        bus.publish(DomainEvent.XpAwarded(ledgerId = 1, amount = 10, sourceType = "QUEST", sourceId = "q1", totalXpAfter = 10))
        bus.publish(
            DomainEvent.QuestCompleted(
                instanceId = 1,
                templateId = 1,
                xp = 40,
                attributeRewardsJson = """{"INT":30}""",
                completedAtEpochMs = 1L,
            ),
        )
        assertEquals(2, received.size)
        assertTrue(received[0] is DomainEvent.XpAwarded)
        assertTrue(received[1] is DomainEvent.QuestCompleted)
    }
}

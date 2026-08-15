package com.example.solo_levelling.ui.levelup

import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LevelUpViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun p_levelUp_setsPendingEvent() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = LevelUpViewModel(bus)
        bus.publish(DomainEvent.LevelUp(1, 2))
        val pending = vm.pendingEvent.value
        assertTrue(pending is LevelUpEvent.LevelUp)
        assertEquals(2, (pending as LevelUpEvent.LevelUp).newLevel)
    }

    @Test
    fun p_questUndone_clearsPendingLevelUp() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = LevelUpViewModel(bus)
        bus.publish(DomainEvent.LevelUp(1, 2))
        assertTrue(vm.pendingEvent.value is LevelUpEvent.LevelUp)

        bus.publish(DomainEvent.QuestUndone(instanceId = 1, xpReversed = 40, undoneAtEpochMs = 1L))
        assertNull(vm.pendingEvent.value)
    }

    @Test
    fun p_xpReversed_clearsPendingLevelUp() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = LevelUpViewModel(bus)
        bus.publish(DomainEvent.LevelUp(1, 2))

        bus.publish(
            DomainEvent.XpReversed(
                ledgerId = 1,
                amount = -40,
                sourceType = "QUEST_UNDO",
                sourceId = "UNDO_1",
                totalXpAfter = 0,
            ),
        )
        assertNull(vm.pendingEvent.value)
    }

    @Test
    fun n_dismiss_clearsPending() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = LevelUpViewModel(bus)
        bus.publish(DomainEvent.RankUp("E", "D"))
        vm.dismiss()
        assertNull(vm.pendingEvent.value)
    }
}

package com.example.solo_levelling.ui.streak

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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreakRecoveryViewModelTest {
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
    fun p_streakBroken_setsPending() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = StreakRecoveryViewModel(bus)
        bus.publish(DomainEvent.StreakBroken(previousStreak = 14, best = 20))
        val pending = vm.pending.value
        assertNotNull(pending)
        assertEquals(14, pending!!.previousStreak)
        assertEquals(20, pending.best)
    }

    @Test
    fun n_dismiss_clearsPending() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = StreakRecoveryViewModel(bus)
        bus.publish(DomainEvent.StreakBroken(7, 14))
        vm.dismiss()
        assertNull(vm.pending.value)
    }

    @Test
    fun e_otherEvents_doNotTrigger() = runTest(dispatcher) {
        val bus = EventBus()
        val vm = StreakRecoveryViewModel(bus)
        bus.publish(DomainEvent.StreakUpdated(0, 10))
        assertNull(vm.pending.value)
    }
}

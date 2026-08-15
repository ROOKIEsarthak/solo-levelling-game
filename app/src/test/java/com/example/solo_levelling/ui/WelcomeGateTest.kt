package com.example.solo_levelling.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeGateTest {
    @Test
    fun p_canEnterApp_whenReadyAndMinDurationMet() {
        assertTrue(canEnterApp(ready = true, elapsedMs = 3_200L, minMs = 3_200L))
    }

    @Test
    fun n_canEnterApp_falseWhenNotReady() {
        assertFalse(canEnterApp(ready = false, elapsedMs = 5_000L, minMs = 3_200L))
    }

    @Test
    fun n_canEnterApp_falseWhenDurationNotMet() {
        assertFalse(canEnterApp(ready = true, elapsedMs = 3_199L, minMs = 3_200L))
    }

    @Test
    fun e_canEnterApp_falseAtZeroElapsedEvenIfReady() {
        assertFalse(canEnterApp(ready = true, elapsedMs = 0L, minMs = 3_200L))
    }

    @Test
    fun e_canEnterApp_trueWhenElapsedExceedsMin() {
        assertTrue(canEnterApp(ready = true, elapsedMs = 4_000L, minMs = 3_200L))
    }

    @Test
    fun p_canEnterApp_defaultMinMsIs3200() {
        assertFalse(canEnterApp(ready = true, elapsedMs = 3_199L))
        assertTrue(canEnterApp(ready = true, elapsedMs = 3_200L))
    }
}

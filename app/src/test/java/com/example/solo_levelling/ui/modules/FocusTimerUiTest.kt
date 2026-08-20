package com.example.solo_levelling.ui.modules

import com.example.solo_levelling.domain.service.EntryValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerUiTest {

    @Test
    fun p_idleStartArmsCountdownFromMinutes() {
        val started = focusTimerStart(25)
        assertEquals(FocusTimerPhase.RUNNING, focusTimerPhase(started.running, started.secondsLeft))
        assertEquals(25 * 60, started.secondsLeft)
        assertTrue(started.running)
    }

    @Test
    fun p_stopKeepsRemainingSeconds() {
        val started = focusTimerStart(25)
        val remaining = started.secondsLeft - 90
        val stopped = focusTimerStop(remaining)
        assertFalse(stopped.running)
        assertEquals(remaining, stopped.secondsLeft)
        assertEquals(FocusTimerPhase.PAUSED, focusTimerPhase(stopped.running, stopped.secondsLeft))
    }

    @Test
    fun p_resumeContinuesRemainingSeconds() {
        val paused = focusTimerStop(140)
        val resumed = focusTimerResume(paused.secondsLeft)
        assertTrue(resumed.running)
        assertEquals(140, resumed.secondsLeft)
        assertEquals(FocusTimerPhase.RUNNING, focusTimerPhase(resumed.running, resumed.secondsLeft))
    }

    @Test
    fun p_restartResetsFromMinutesNotRemaining() {
        val paused = focusTimerStop(40)
        val restarted = focusTimerStart(10)
        assertTrue(restarted.running)
        assertEquals(10 * 60, restarted.secondsLeft)
        assertEquals(FocusTimerPhase.RUNNING, focusTimerPhase(restarted.running, restarted.secondsLeft))
        assertTrue(restarted.secondsLeft > paused.secondsLeft)
    }

    @Test
    fun p_buttonLabelsMatchPhase() {
        assertEquals("START TIMER", focusTimerPrimaryLabel(FocusTimerPhase.IDLE))
        assertEquals("LOG NOW", focusTimerSecondaryLabel(FocusTimerPhase.IDLE))
        assertEquals("STOP TIMER", focusTimerPrimaryLabel(FocusTimerPhase.RUNNING))
        assertEquals("LOG NOW", focusTimerSecondaryLabel(FocusTimerPhase.RUNNING))
        assertEquals("RESUME", focusTimerPrimaryLabel(FocusTimerPhase.PAUSED))
        assertEquals("RESTART", focusTimerSecondaryLabel(FocusTimerPhase.PAUSED))
    }

    @Test
    fun p_statusLabels() {
        assertEquals("RUNNING", focusTimerStatusLabel(FocusTimerPhase.RUNNING, hasLoggedToday = false))
        assertEquals("PAUSED", focusTimerStatusLabel(FocusTimerPhase.PAUSED, hasLoggedToday = true))
        assertEquals("LOGGED", focusTimerStatusLabel(FocusTimerPhase.IDLE, hasLoggedToday = true))
        assertEquals("IDLE", focusTimerStatusLabel(FocusTimerPhase.IDLE, hasLoggedToday = false))
    }

    @Test
    fun n_invalidMinutesBlocksStartAndRestart() {
        assertEquals("Enter a valid minutes", EntryValidation.requirePositiveInt("", "minutes"))
        assertEquals("Enter a valid minutes", EntryValidation.requirePositiveInt("abc", "minutes"))
        assertEquals("minutes must be greater than 0", EntryValidation.requirePositiveInt("0", "minutes"))
        assertNull(EntryValidation.requirePositiveInt("25", "minutes"))
    }

    @Test
    fun e_zeroSecondsIsIdleNotPaused() {
        assertEquals(FocusTimerPhase.IDLE, focusTimerPhase(running = false, secondsLeft = 0))
        assertEquals(FocusTimerPhase.RUNNING, focusTimerPhase(running = true, secondsLeft = 0))
    }

    @Test
    fun e_oneSecondRemainingIsPaused() {
        assertEquals(FocusTimerPhase.PAUSED, focusTimerPhase(running = false, secondsLeft = 1))
    }

    @Test
    fun e_clockFormatsMinutesAndPaddedSeconds() {
        assertEquals("0:00", formatFocusTimerClock(0))
        assertEquals("0:05", formatFocusTimerClock(5))
        assertEquals("1:05", formatFocusTimerClock(65))
        assertEquals("25:00", formatFocusTimerClock(1500))
    }

    @Test
    fun e_completePathReturnsToIdleWithoutKeepingSeconds() {
        val completed = FocusTimerState(running = false, secondsLeft = 0)
        assertEquals(FocusTimerPhase.IDLE, focusTimerPhase(completed.running, completed.secondsLeft))
        assertEquals("IDLE", focusTimerStatusLabel(FocusTimerPhase.IDLE, hasLoggedToday = false))
        assertEquals("LOGGED", focusTimerStatusLabel(FocusTimerPhase.IDLE, hasLoggedToday = true))
    }

    @Test
    fun p_journalSectionScrollsToJournalY() {
        assertEquals(420, modulesScrollTargetY("journal", focusY = 10, metricsY = 80, journalY = 420))
    }

    @Test
    fun p_focusSectionScrollsToFocusY() {
        assertEquals(10, modulesScrollTargetY("focus", focusY = 10, metricsY = 80, journalY = 420))
    }

    @Test
    fun p_metricsSectionScrollsToMetricsY() {
        assertEquals(80, modulesScrollTargetY("metrics", focusY = 10, metricsY = 80, journalY = 420))
    }

    @Test
    fun n_unknownSectionDoesNotScroll() {
        assertEquals(0, modulesScrollTargetY("", focusY = 10, metricsY = 80, journalY = 420))
        assertEquals(0, modulesScrollTargetY("bosses", focusY = 10, metricsY = 80, journalY = 420))
    }
}

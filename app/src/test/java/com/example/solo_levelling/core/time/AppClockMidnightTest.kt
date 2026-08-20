package com.example.solo_levelling.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AppClockMidnightTest {
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val newYork = ZoneId.of("America/New_York")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun p_beforeMidnight_todayIsCurrentDay() {
        val clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 8, 15), LocalTime.of(23, 59, 59), kolkata))
        assertEquals(LocalDate.of(2026, 8, 15), clock.today(kolkata))
    }

    @Test
    fun p_afterMidnight_todayIsNextDay() {
        val clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(0, 0, 1), kolkata))
        assertEquals(LocalDate.of(2026, 8, 16), clock.today(kolkata))
    }

    @Test
    fun p_nextMidnight_asiaKolkata() {
        val clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 8, 15), LocalTime.of(14, 30), kolkata))
        val next = InstantMs(clock.nextLocalMidnightEpochMs(kolkata))
        val expected = ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.MIDNIGHT, kolkata)
            .toInstant().toEpochMilli()
        assertEquals(expected, next)
        assertTrue(clock.millisUntilNextLocalMidnight(kolkata) > 0)
    }

    @Test
    fun p_nextMidnight_americaNewYork() {
        val clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 3, 10), LocalTime.of(10, 0), newYork))
        val expected = ZonedDateTime.of(LocalDate.of(2026, 3, 11), LocalTime.MIDNIGHT, newYork)
            .toInstant().toEpochMilli()
        assertEquals(expected, clock.nextLocalMidnightEpochMs(newYork))
    }

    @Test
    fun p_nextMidnight_asiaTokyo() {
        val clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 12, 31), LocalTime.of(23, 0), tokyo))
        val expected = ZonedDateTime.of(LocalDate.of(2027, 1, 1), LocalTime.MIDNIGHT, tokyo)
            .toInstant().toEpochMilli()
        assertEquals(expected, clock.nextLocalMidnightEpochMs(tokyo))
    }

    @Test
    fun e_exactlyAtMidnight_delayIsFullDay() {
        val clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 8, 15), LocalTime.MIDNIGHT, kolkata))
        val delay = clock.millisUntilNextLocalMidnight(kolkata)
        // At exactly 00:00:00, next midnight is tomorrow = ~24h
        assertEquals(24L * 60 * 60 * 1000, delay)
    }

    @Test
    fun e_timezoneChange_recalculatesNextMidnight() {
        val clock = FakeAppClock()
        val instant = ZonedDateTime.of(LocalDate.of(2026, 8, 15), LocalTime.of(12, 0), kolkata).toInstant()
        clock.epochMs = instant.toEpochMilli()
        val delayKolkata = clock.millisUntilNextLocalMidnight(kolkata)
        val delayNy = clock.millisUntilNextLocalMidnight(newYork)
        assertTrue(delayKolkata != delayNy)
    }

    @Test
    fun e_localDateFromEpoch() {
        val clock = FakeAppClock()
        val zdt = ZonedDateTime.of(LocalDateTime.of(2026, 8, 15, 23, 30), kolkata)
        assertEquals(LocalDate.of(2026, 8, 15), clock.localDate(zdt.toInstant().toEpochMilli(), kolkata))
    }

    private fun InstantMs(ms: Long) = ms
}

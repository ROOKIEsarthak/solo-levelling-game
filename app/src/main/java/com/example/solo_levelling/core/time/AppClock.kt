package com.example.solo_levelling.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface AppClock {
    fun nowEpochMs(): Long
    fun today(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate

    fun localDate(epochMs: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()

    fun nextLocalMidnightEpochMs(zoneId: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowEpochMs()).atZone(zoneId)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId)
        return nextMidnight.toInstant().toEpochMilli()
    }

    fun millisUntilNextLocalMidnight(zoneId: ZoneId): Long =
        (nextLocalMidnightEpochMs(zoneId) - nowEpochMs()).coerceAtLeast(0L)
}

class SystemAppClock : AppClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()

    override fun today(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(nowEpochMs()).atZone(zoneId).toLocalDate()
}

class FakeAppClock(
    var epochMs: Long = System.currentTimeMillis(),
    var fixedDate: LocalDate? = null,
) : AppClock {
    override fun nowEpochMs(): Long = epochMs

    override fun today(zoneId: ZoneId): LocalDate =
        fixedDate ?: Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()

    fun setZoned(dateTime: ZonedDateTime) {
        epochMs = dateTime.toInstant().toEpochMilli()
        fixedDate = null
    }
}

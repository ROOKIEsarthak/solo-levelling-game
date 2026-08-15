package com.example.solo_levelling.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface AppClock {
    fun nowEpochMs(): Long
    fun today(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate
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
}

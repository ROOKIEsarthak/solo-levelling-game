package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDaysToCsvTest {
    @Test
    fun p_mapsMonThroughSun() {
        assertEquals("1,2,3,4,5,6,7", scheduleDaysToCsv(listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")))
    }

    @Test
    fun p_weekdaySubset() {
        assertEquals("1,2,3,4,5", scheduleDaysToCsv(listOf("MON", "TUE", "WED", "THU", "FRI")))
    }

    @Test
    fun n_unknownDaysSkipped() {
        assertEquals("1,3", scheduleDaysToCsv(listOf("MON", "INVALID", "WED")))
    }

    @Test
    fun e_emptyListReturnsEmpty() {
        assertEquals("", scheduleDaysToCsv(emptyList()))
    }
}

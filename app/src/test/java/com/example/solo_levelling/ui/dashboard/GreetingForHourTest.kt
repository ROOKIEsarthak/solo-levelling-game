package com.example.solo_levelling.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingForHourTest {
    @Test
    fun p_morningAfternoonEvening() {
        assertEquals("GOOD MORNING", greetingForHour(8))
        assertEquals("GOOD AFTERNOON", greetingForHour(14))
        assertEquals("GOOD EVENING", greetingForHour(19))
    }

    @Test
    fun e_nightAndBoundaries() {
        assertEquals("GOOD NIGHT", greetingForHour(2))
        assertEquals("GOOD MORNING", greetingForHour(5))
        assertEquals("GOOD AFTERNOON", greetingForHour(12))
        assertEquals("GOOD EVENING", greetingForHour(17))
        assertEquals("GOOD NIGHT", greetingForHour(22))
    }
}

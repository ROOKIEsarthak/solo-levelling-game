package com.example.solo_levelling.ui.dashboard

import com.example.solo_levelling.ui.components.greetingForHour
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingForHourTest {
    @Test
    fun p_morningAfternoonEvening() {
        assertEquals("Good morning", greetingForHour(8))
        assertEquals("Good afternoon", greetingForHour(14))
        assertEquals("Good evening", greetingForHour(19))
    }

    @Test
    fun e_nightAndBoundaries() {
        assertEquals("Welcome back", greetingForHour(2))
        assertEquals("Good morning", greetingForHour(5))
        assertEquals("Good afternoon", greetingForHour(12))
        assertEquals("Good evening", greetingForHour(17))
        assertEquals("Welcome back", greetingForHour(22))
    }
}

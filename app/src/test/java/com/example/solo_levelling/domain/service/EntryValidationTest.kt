package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryValidationTest {
    @Test
    fun p_requireNonBlank_okWhenPresent() {
        assertNull(EntryValidation.requireNonBlank("Oats", "food name"))
    }

    @Test
    fun n_requireNonBlank_rejectsEmptyAndBlank() {
        assertEquals("Enter food name", EntryValidation.requireNonBlank("", "food name"))
        assertEquals("Enter food name", EntryValidation.requireNonBlank("   ", "food name"))
        assertEquals("Enter food name", EntryValidation.requireNonBlank(null, "food name"))
    }

    @Test
    fun p_requirePositiveFloat_ok() {
        assertNull(EntryValidation.requirePositiveFloat("60", "quantity"))
        assertNull(EntryValidation.requirePositiveFloat("0.5", "weight"))
    }

    @Test
    fun n_requirePositiveFloat_rejectsInvalidAndZero() {
        assertEquals("Enter a valid quantity", EntryValidation.requirePositiveFloat("", "quantity"))
        assertEquals("Enter a valid quantity", EntryValidation.requirePositiveFloat("abc", "quantity"))
        assertEquals("quantity must be greater than 0", EntryValidation.requirePositiveFloat("0", "quantity"))
        assertEquals("quantity must be greater than 0", EntryValidation.requirePositiveFloat("-1", "quantity"))
    }

    @Test
    fun p_requirePositiveInt_ok() {
        assertNull(EntryValidation.requirePositiveInt("10", "reps"))
    }

    @Test
    fun n_requirePositiveInt_rejectsInvalidAndZero() {
        assertEquals("Enter a valid reps", EntryValidation.requirePositiveInt("", "reps"))
        assertEquals("reps must be greater than 0", EntryValidation.requirePositiveInt("0", "reps"))
    }

    @Test
    fun e_requireNonNegativeInt_allowsZero() {
        assertNull(EntryValidation.requireNonNegativeInt("0", "calories"))
        assertEquals("calories cannot be negative", EntryValidation.requireNonNegativeInt("-1", "calories"))
    }

    @Test
    fun p_firstError_returnsFirstNonNull() {
        assertEquals(
            "Enter name",
            EntryValidation.firstError(
                EntryValidation.requireNonBlank("", "name"),
                EntryValidation.requirePositiveFloat("1", "qty"),
            ),
        )
        assertNull(
            EntryValidation.firstError(
                EntryValidation.requireNonBlank("x", "name"),
                EntryValidation.requirePositiveFloat("1", "qty"),
            ),
        )
    }

    @Test
    fun e_messages_areSpecific() {
        val msg = EntryValidation.requirePositiveFloat(null, "protein")!!
        assertTrue(msg.contains("protein"))
    }
}

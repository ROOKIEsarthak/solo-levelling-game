package com.example.solo_levelling.ui.fitness

import org.junit.Assert.assertEquals
import org.junit.Test

class FitnessBiometricFormatTest {
    @Test
    fun p_formatBiometricMeasure_joinsValueAndUnit() {
        assertEquals("170.0 cm", formatBiometricMeasure("170.0", "cm"))
        assertEquals("70.0 kg", formatBiometricMeasure("70.0", "kg"))
    }

    @Test
    fun n_formatBiometricMeasure_blankValue() {
        assertEquals("", formatBiometricMeasure("", "cm"))
        assertEquals("", formatBiometricMeasure("   ", "kg"))
    }

    @Test
    fun e_formatBiometricMeasure_blankUnitKeepsValue() {
        assertEquals("24.2", formatBiometricMeasure("24.2", ""))
        assertEquals("24.2", formatBiometricMeasure(" 24.2 ", "  "))
    }

    @Test
    fun e_formatBiometricMeasure_trimsWhitespace() {
        assertEquals("170.0 cm", formatBiometricMeasure(" 170.0 ", " cm "))
    }

    @Test
    fun p_formatFitnessGoalDisplay_replacesUnderscores() {
        assertEquals("maintenance", formatFitnessGoalDisplay("maintenance"))
        assertEquals("fat loss", formatFitnessGoalDisplay("fat_loss"))
        assertEquals("muscle gain", formatFitnessGoalDisplay("muscle_gain"))
    }

    @Test
    fun n_formatFitnessGoalDisplay_blank() {
        assertEquals("", formatFitnessGoalDisplay(""))
        assertEquals("", formatFitnessGoalDisplay("   "))
    }

    @Test
    fun e_formatFitnessGoalDisplay_trims() {
        assertEquals("maintenance", formatFitnessGoalDisplay("  maintenance  "))
    }
}

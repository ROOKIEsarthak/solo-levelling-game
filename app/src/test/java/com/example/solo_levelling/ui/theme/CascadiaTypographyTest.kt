package com.example.solo_levelling.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CascadiaTypographyTest {
    @Test
    fun p_allTypographySlotsUseCascadiaCode() {
        val styles = allTypographyStyles()
        assertEquals(15, styles.size)
        styles.forEach { style ->
            assertEquals(CascadiaCode, style.fontFamily)
        }
    }

    @Test
    fun n_defaultMaterialTypographyIsNotUsedAsAppTypography() {
        // App Typography must not leave bodyLarge on platform Default.
        assertTrue(Typography.bodyLarge.fontFamily === CascadiaCode)
    }

    @Test
    fun e_everySlotSharesSameFontFamilyInstance() {
        val families = allTypographyStyles().map { it.fontFamily }.distinct()
        assertEquals(listOf(CascadiaCode), families)
    }
}

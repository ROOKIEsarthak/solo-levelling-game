package com.example.solo_levelling.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CascadiaTypographyTest {
    @Test
    fun p_humanSlotsUseInter() {
        allInterTypographyStyles().forEach { style ->
            assertEquals(Inter, style.fontFamily)
        }
    }

    @Test
    fun p_systemLabelSlotsUseJetBrainsMono() {
        allMonoTypographyStyles().forEach { style ->
            assertEquals(JetBrainsMono, style.fontFamily)
        }
    }

    @Test
    fun n_defaultMaterialTypographyIsNotUsedAsAppTypography() {
        assertTrue(Typography.bodyLarge.fontFamily === Inter)
        assertTrue(Typography.labelMedium.fontFamily === JetBrainsMono)
    }

    @Test
    fun e_cascadiaAliasMapsToMono() {
        assertEquals(JetBrainsMono, CascadiaCode)
    }

    @Test
    fun e_allTypographyStylesCount() {
        assertEquals(15, allTypographyStyles().size)
    }
}

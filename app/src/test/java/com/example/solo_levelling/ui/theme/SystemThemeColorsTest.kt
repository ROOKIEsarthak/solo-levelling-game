package com.example.solo_levelling.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemThemeColorsTest {
    @Test
    fun p_darkSchemeUsesBluePrimaryAndBackground() {
        assertEquals(Color(0xFF4DA3FF), SystemPrimary)
        assertEquals(Color(0xFF05070D), SystemBackground)
        assertEquals(SystemPrimary, DarkColorScheme.primary)
        assertEquals(SystemBackground, DarkColorScheme.background)
        assertEquals(SystemSurface, DarkColorScheme.surfaceContainer)
        assertEquals(SystemSurface2, DarkColorScheme.surfaceContainerHigh)
        assertEquals(SystemError, DarkColorScheme.error)
    }

    @Test
    fun n_semanticTokensAreDistinctFromEachOther() {
        assertNotEquals(SystemSuccess, SystemWarning)
        assertNotEquals(SystemSuccess, SystemError)
        assertNotEquals(SystemPrimary, SystemBackground)
        assertNotEquals(SystemPrimary, SystemSecondary)
    }

    @Test
    fun e_splashGlowTokensMatchBrandHex() {
        assertEquals(Color(0xFF05070D), SplashBackground)
        assertEquals(Color(0xFF7C6CFF), GlowPurple)
        assertEquals(Color(0xFF67D4FF), GlowCyan)
        assertEquals(Color(0xFF67D4FF), SystemCyan)
    }

    @Test
    fun e_outlineIsLowAlphaWhite() {
        assertTrue(SystemOutline.alpha < 0.2f)
        assertTrue(SystemOutline.red > 0.9f)
    }

    @Test
    fun p_onPrimaryContrastsAgainstPrimary() {
        assertEquals(SystemOnPrimary, DarkColorScheme.onPrimary)
        assertNotEquals(DarkColorScheme.primary, DarkColorScheme.onPrimary)
    }

    @Test
    fun e_cardAndElevatedSurfacesMatchBrief() {
        assertEquals(Color(0xFF0D1320), SystemSurface)
        assertEquals(Color(0xFF111827), SystemSurface2)
        assertEquals(Color(0xFF080C16), SystemSecondaryBackground)
    }
}

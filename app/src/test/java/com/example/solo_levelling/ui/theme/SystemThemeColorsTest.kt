package com.example.solo_levelling.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemThemeColorsTest {
    @Test
    fun p_darkSchemeUsesSovereignPrimaryAndBackground() {
        assertEquals(Color(0xFFA2C9FF), SystemPrimary)
        assertEquals(Color(0xFF4DA3FF), SystemPrimaryContainer)
        assertEquals(Color(0xFF05070D), SystemBackground)
        assertEquals(SystemPrimary, DarkColorScheme.primary)
        assertEquals(SystemBackground, DarkColorScheme.background)
        assertEquals(SystemSurface, DarkColorScheme.surfaceContainer)
        assertEquals(SystemError, DarkColorScheme.error)
    }

    @Test
    fun n_semanticTokensAreDistinctFromEachOther() {
        assertNotEquals(SystemSuccess, SystemWarning)
        assertNotEquals(SystemSuccess, SystemError)
        assertNotEquals(SystemPrimary, SystemBackground)
        assertNotEquals(SystemPrimary, SystemSecondary)
        assertNotEquals(SystemTertiary, SystemPrimary)
    }

    @Test
    fun e_splashGlowTokensMatchBrandHex() {
        assertEquals(Color(0xFF05070D), SplashBackground)
        assertEquals(Color(0xFF7C6CFF), GlowPurple)
        assertEquals(Color(0xFF4DA3FF), GlowCyan)
        assertEquals(Color(0xFF4DA3FF), SystemCyan)
    }

    @Test
    fun e_outlineHasVisibleAlpha() {
        assertTrue(SystemOutline.alpha > 0f)
        assertTrue(SystemOutline.alpha < 0.5f)
    }

    @Test
    fun p_onPrimaryContrastsAgainstPrimary() {
        assertEquals(SystemOnPrimary, DarkColorScheme.onPrimary)
        assertNotEquals(DarkColorScheme.primary, DarkColorScheme.onPrimary)
    }

    @Test
    fun e_cardAndElevatedSurfacesMatchBrief() {
        assertEquals(Color(0xFF0D1320), SystemSurface)
        assertEquals(Color(0xFF11131A), SystemSurface2)
        assertEquals(Color(0xFFEDC146), SystemTertiary)
        assertEquals(Color(0xFFC6BFFF), SystemSecondary)
    }
}

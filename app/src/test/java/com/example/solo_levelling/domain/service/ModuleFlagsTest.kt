package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleFlagsTest {
    @Test
    fun p_parse_trueFlags() {
        val m = ModuleFlags.parse("true", "false", "1")
        assertTrue(m.career)
        assertFalse(m.workout)
        assertTrue(m.diet)
        assertTrue(m.anyEnabled)
    }

    @Test
    fun n_parse_missingIsFalse() {
        val m = ModuleFlags.parse(null, null, null)
        assertFalse(m.anyEnabled)
    }

    @Test
    fun e_resolve_onboardedMissing_migratesAllOn() {
        val m = ModuleFlags.resolve(
            onboardingDone = true,
            career = null,
            workout = null,
            diet = null,
        )
        assertEquals(EnabledModules(career = true, workout = true, diet = true), m)
    }

    @Test
    fun e_resolve_notOnboardedMissing_allOff() {
        val m = ModuleFlags.resolve(
            onboardingDone = false,
            career = null,
            workout = null,
            diet = null,
        )
        assertFalse(m.anyEnabled)
    }

    @Test
    fun p_resolve_explicitFlagsRespected() {
        val m = ModuleFlags.resolve(
            onboardingDone = true,
            career = "true",
            workout = "false",
            diet = "false",
        )
        assertTrue(m.career)
        assertFalse(m.workout)
        assertFalse(m.diet)
    }

    @Test
    fun p_encode_roundTripKeys() {
        val encoded = ModuleFlags.encode(EnabledModules(career = true, workout = false, diet = true))
        assertEquals("true", encoded[ModuleFlags.KEY_CAREER])
        assertEquals("false", encoded[ModuleFlags.KEY_WORKOUT])
        assertEquals("true", encoded[ModuleFlags.KEY_DIET])
    }

    @Test
    fun n_needsMigration_whenAnyNull() {
        assertTrue(ModuleFlags.needsMigration(null, "true", "true"))
        assertFalse(ModuleFlags.needsMigration("false", "false", "false"))
    }
}

package com.example.solo_levelling.ui.dashboard

import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.AnalyticsService
import com.example.solo_levelling.domain.service.EnabledModules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMetricsModuleIsolationTest {
    @Test
    fun p_fitnessOnly_excludesIntelligence() {
        val modules = EnabledModules(career = false, workout = true, diet = false)
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.STR.name, modules))
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.FOC.name, modules))
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.INT.name, modules))
    }

    @Test
    fun p_fitnessAndDiet_excludesCareerIntelligence() {
        val modules = EnabledModules(career = false, workout = true, diet = true)
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.VIT.name, modules))
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.INT.name, modules))
    }

    @Test
    fun p_careerOnly_excludesStrength() {
        val modules = EnabledModules(career = true, workout = false, diet = false)
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.INT.name, modules))
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.STR.name, modules))
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.END.name, modules))
    }

    @Test
    fun e_snapshot_bottomIgnoresDisabledModuleAttrs() {
        val modules = EnabledModules(career = false, workout = true, diet = false)
        val snap = AnalyticsService.attributeSnapshot(
            attrs = listOf(
                AttributeCode.STR.name to 90,
                AttributeCode.INT.name to 10,
                AttributeCode.FOC.name to 40,
            ),
            modules = modules,
        )
        assertTrue(snap.bottomCode == AttributeCode.FOC.name || snap.bottomCode == AttributeCode.STR.name)
        assertFalse(snap.bottomCode == AttributeCode.INT.name)
    }
}

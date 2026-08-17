package com.example.solo_levelling.ui.onboarding

import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSplitDayMapTest {
    @Test
    fun p_defaultDayMap_isValidForPplUl() {
        val split = WorkoutCatalog.findSplit("ppl_ul")!!
        val map = WorkoutSplitLogic.defaultDayMap(split)
        assertTrue(isSplitDayMapValid("ppl_ul", map))
        assertNull(splitDayMapValidationMessage("ppl_ul", map))
    }

    @Test
    fun p_scheduleDaysFromSplitDayMap_mapsIsoToMonLabels() {
        val map = mapOf(1 to 1, 2 to 3, 3 to 5)
        assertEquals(listOf("MON", "WED", "FRI"), scheduleDaysFromSplitDayMap(map))
    }

    @Test
    fun n_isSplitDayMapValid_rejectsDuplicateWeekdays() {
        val map = mapOf(1 to 1, 2 to 1, 3 to 3, 4 to 5, 5 to 7)
        assertFalse(isSplitDayMapValid("ppl_ul", map))
        assertEquals(
            "Each weekday can only have one workout",
            splitDayMapValidationMessage("ppl_ul", map),
        )
    }

    @Test
    fun n_isSplitDayMapValid_rejectsMissingSlots() {
        val map = mapOf(1 to 1, 2 to 2)
        assertFalse(isSplitDayMapValid("ppl_ul", map))
        assertEquals(
            "Assign a weekday for every workout",
            splitDayMapValidationMessage("ppl_ul", map),
        )
    }

    @Test
    fun e_scheduleDaysFromSplitDayMap_emptyMap() {
        assertEquals(emptyList<String>(), scheduleDaysFromSplitDayMap(emptyMap()))
    }
}

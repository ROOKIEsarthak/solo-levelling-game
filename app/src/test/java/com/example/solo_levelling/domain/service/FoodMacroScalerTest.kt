package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.seed.FoodCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodMacroScalerTest {
    private val oats = FoodCatalog.findById("oats")!!
    private val whey = FoodCatalog.findById("whey_protein")!!
    private val egg = FoodCatalog.findById("egg_whole")!!
    private val soya = FoodCatalog.findById("soya_chunks")!!

    @Test
    fun p_oats100g_matchesReference() {
        val scaled = FoodMacroScaler.scale(oats, 100f)
        assertEquals(389, scaled.calories)
        assertEquals(17, scaled.protein)
        assertEquals(66, scaled.carbs)
        assertEquals(7, scaled.fat)
        assertEquals("g", scaled.defaultUnit)
    }

    @Test
    fun p_oats60g_scalesProportionally() {
        val scaled = FoodMacroScaler.scale(oats, 60f)
        assertEquals(233, scaled.calories)
        assertEquals(10, scaled.protein)
        assertEquals(40, scaled.carbs)
        assertEquals(4, scaled.fat)
    }

    @Test
    fun p_wheyOneScoop_matchesReference() {
        val scaled = FoodMacroScaler.scale(whey, 1f)
        assertEquals(120, scaled.calories)
        assertEquals(24, scaled.protein)
        assertEquals(3, scaled.carbs)
        assertEquals(2, scaled.fat)
        assertEquals("scoop", scaled.defaultUnit)
    }

    @Test
    fun p_wheyTwoScoops_doublesMacros() {
        val scaled = FoodMacroScaler.scale(whey, 2f)
        assertEquals(240, scaled.calories)
        assertEquals(48, scaled.protein)
        assertEquals(6, scaled.carbs)
        assertEquals(4, scaled.fat)
    }

    @Test
    fun p_wholeEggTimesTwo() {
        val scaled = FoodMacroScaler.scale(egg, 2f)
        assertEquals(144, scaled.calories)
        assertEquals(13, scaled.protein)
        assertEquals(1, scaled.carbs)
        assertEquals(10, scaled.fat)
        assertEquals("egg", scaled.defaultUnit)
    }

    @Test
    fun p_soyaChunks100gDry_usesPer100gBasis() {
        val scaled = FoodMacroScaler.scale(soya, 50f)
        assertEquals(173, scaled.calories)
        assertEquals(26, scaled.protein)
    }

    @Test
    fun n_zeroQuantity_returnsZeros() {
        val scaled = FoodMacroScaler.scale(oats, 0f)
        assertEquals(0, scaled.calories)
        assertEquals(0, scaled.protein)
        assertEquals(0, scaled.carbs)
        assertEquals(0, scaled.fat)
    }

    @Test
    fun n_negativeQuantity_returnsZeros() {
        val scaled = FoodMacroScaler.scale(whey, -1f)
        assertEquals(0, scaled.calories)
        assertEquals(0, scaled.protein)
    }

    @Test
    fun e_defaultQuantity_per100gIs100_pieceIs1() {
        assertEquals(100f, FoodMacroScaler.defaultQuantity(oats))
        assertEquals(1f, FoodMacroScaler.defaultQuantity(whey))
    }

    @Test
    fun e_previewLine_includesMacrosAndAvoidsEntityDump() {
        val line = FoodMacroScaler.previewLine(oats, 60f)
        assertTrue(line.contains("Oats"))
        assertTrue(line.contains("233 kcal"))
        assertTrue(!line.contains("FoodCatalogEntry"))
    }

    @Test
    fun p_whiteBreadOneSlice_matchesReference() {
        val bread = FoodCatalog.findById("bread_white")!!
        val scaled = FoodMacroScaler.scale(bread, 1f)
        assertEquals(67, scaled.calories)
        assertEquals(2, scaled.protein)
        assertEquals(12, scaled.carbs)
        assertEquals(1, scaled.fat)
        assertEquals("slice", scaled.defaultUnit)
        assertEquals(1f, FoodMacroScaler.defaultQuantity(bread))
    }

    @Test
    fun p_brownBreadTwoSlices_scalesByCount() {
        val bread = FoodCatalog.findById("bread_brown")!!
        val scaled = FoodMacroScaler.scale(bread, 2f)
        assertEquals(124, scaled.calories)
        assertEquals(7, scaled.protein)
        assertEquals(21, scaled.carbs)
        assertEquals(2, scaled.fat)
    }

    @Test
    fun n_breadSearch_findsByCategory() {
        val hits = FoodCatalog.search("bread")
        assertTrue(hits.any { it.id == "bread_white" })
        assertTrue(hits.any { it.id == "bread_brown" })
    }
}

package com.example.solo_levelling.domain.service

import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.AttributeDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttributeRewardsParserTest {
    @Test
    fun p_parse_readsAttributeMap() {
        val deltas = AttributeRewardsParser.parse("""{"INT":30,"DISC":10}""")
        assertEquals(2, deltas.size)
        assertEquals(30, deltas.first { it.code == AttributeCode.INT }.amount)
        assertEquals(10, deltas.first { it.code == AttributeCode.DISC }.amount)
    }

    @Test
    fun n_parse_blankReturnsEmpty() {
        assertTrue(AttributeRewardsParser.parse("").isEmpty())
        assertTrue(AttributeRewardsParser.parse("   ").isEmpty())
    }

    @Test
    fun e_parse_ignoresZeroAmounts() {
        val deltas = AttributeRewardsParser.parse("""{"INT":0,"STR":5}""")
        assertEquals(1, deltas.size)
        assertEquals(AttributeCode.STR, deltas[0].code)
    }

    @Test
    fun p_toJson_roundTrips() {
        val json = AttributeRewardsParser.toJson(listOf(AttributeDelta(AttributeCode.FOC, 12)))
        val parsed = AttributeRewardsParser.parse(json)
        assertEquals(12, parsed.single().amount)
        assertEquals(AttributeCode.FOC, parsed.single().code)
    }
}

package com.example.solo_levelling.ui.character

import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterScreenTest {
    @Test
    fun p_visibleLedgerEntries_collapsed() {
        val ledger = (1..8).map { i ->
            XpLedgerEntryEntity(
                id = i.toLong(),
                amount = 10,
                sourceType = "QUEST",
                sourceId = "q$i",
                createdAtEpochMs = i.toLong(),
            )
        }
        assertEquals(5, visibleLedgerEntries(ledger, expanded = false, collapsedLimit = 5).size)
    }

    @Test
    fun p_visibleLedgerEntries_expanded() {
        val ledger = (1..8).map { i ->
            XpLedgerEntryEntity(
                id = i.toLong(),
                amount = 10,
                sourceType = "QUEST",
                sourceId = "q$i",
                createdAtEpochMs = i.toLong(),
            )
        }
        assertEquals(8, visibleLedgerEntries(ledger, expanded = true, collapsedLimit = 5).size)
    }

    @Test
    fun e_visibleLedgerEntries_shortList() {
        val ledger = listOf(
            XpLedgerEntryEntity(id = 1, amount = 5, sourceType = "QUEST", sourceId = "q1", createdAtEpochMs = 100L),
        )
        assertEquals(1, visibleLedgerEntries(ledger, expanded = false, collapsedLimit = 5).size)
    }

    @Test
    fun p_ledgerXpLabel_positive() {
        assertEquals("+25 XP", ledgerXpLabel(25))
    }

    @Test
    fun n_ledgerXpLabel_negative() {
        assertEquals("-10 XP", ledgerXpLabel(-10))
    }

    @Test
    fun e_ledgerXpLabel_zero() {
        assertEquals("+0 XP", ledgerXpLabel(0))
    }
}

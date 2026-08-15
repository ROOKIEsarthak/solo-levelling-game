package com.example.solo_levelling.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreferJapaneseVoiceNameTest {
    @Test
    fun p_prefersExplicitMaleVoice() {
        assertEquals(
            "ja-jp-x-male-local",
            preferJapaneseVoiceName(
                listOf("ja-jp-x-female-local", "ja-jp-x-male-local", "ja-jp-standard"),
            ),
        )
    }

    @Test
    fun n_returnsNullWhenEmpty() {
        assertNull(preferJapaneseVoiceName(emptyList()))
    }

    @Test
    fun e_skipsFemaleWhenNoMaleTagged() {
        assertEquals(
            "ja-jp-x-jab-local",
            preferJapaneseVoiceName(
                listOf("ja-jp-x-female-local", "ja-jp-x-jab-local"),
            ),
        )
    }

    @Test
    fun e_fallsBackToFirstWhenOnlyFemale() {
        assertEquals(
            "ja-jp-x-female-local",
            preferJapaneseVoiceName(listOf("ja-jp-x-female-local")),
        )
    }
}

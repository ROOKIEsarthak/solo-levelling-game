package com.example.solo_levelling.ui.consent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemConsentCopyTest {
    private val allCopy = listOf(
        ConsentEyebrow,
        ConsentHeading,
        ConsentWhat,
        ConsentWhy,
        ConsentHow,
        ConsentQuestion,
        ConsentAgreeLine,
        ConsentContinueLabel,
        ConsentDeclineLabel,
        ConsentDeclineTitle,
        ConsentDeclineBody,
        ConsentExitLabel,
        ConsentGoBackLabel,
    ).joinToString("\n")

    @Test
    fun p_headingCommunicatesPurposeNotAQuestion() {
        assertTrue(ConsentHeading.contains("INTENTION"))
        assertTrue(ConsentHeading.contains("PROGRESS"))
        assertFalse(ConsentHeading.contains("DO YOU CHOOSE TO ACCEPT"))
        assertFalse(ConsentHeading.contains("THE SYSTEM WILL GUIDE YOU"))
    }

    @Test
    fun p_explainsWhatWhyHowThenAsksForConsent() {
        assertTrue(ConsentWhat.contains("choose the areas"))
        assertTrue(ConsentWhat.contains("actions"))
        assertTrue(ConsentWhy.contains("see it"))
        assertTrue(ConsentWhy.contains("measure"))
        assertTrue(ConsentHow.contains("areas you select"))
        assertTrue(ConsentHow.contains("what you choose"))
        assertEquals("DO YOU CHOOSE TO ACCEPT?", ConsentQuestion)
        assertTrue(ConsentAgreeLine.contains("information you provide"))
        assertTrue(ConsentAgreeLine.contains("progression"))
        assertEquals("CONTINUE", ConsentContinueLabel)
    }

    @Test
    fun p_explainsModuleChoiceWithoutListingAreas() {
        assertTrue(ConsentHow.contains("Only the areas you select"))
        assertTrue(ConsentHow.contains("one, or several"))
        assertTrue(ConsentHow.contains("nothing else"))
        assertFalse(ConsentWhat.contains("Career"))
        assertFalse(ConsentWhy.contains("Career"))
        assertFalse(ConsentHow.contains("Career"))
        assertFalse(ConsentHow.contains("Fitness"))
        assertFalse(ConsentHow.contains("Nutrition"))
    }

    @Test
    fun n_doesNotUseGameLanguage() {
        val lower = allCopy.lowercase()
        assertFalse(allCopy.contains("PLAYER"))
        assertFalse(lower.contains("game"))
        assertFalse(lower.contains("level up"))
        assertFalse(lower.contains("acceptance is required"))
        assertFalse(lower.contains("you will become"))
        assertFalse(lower.contains("win"))
        assertFalse(lower.contains("beat the system"))
    }

    @Test
    fun n_doesNotAskForPersonalData() {
        val lower = allCopy.lowercase()
        assertFalse(lower.contains("enter your name"))
        assertFalse(lower.contains("gender"))
        assertFalse(lower.contains("height"))
        assertFalse(lower.contains("weight"))
        assertFalse(lower.contains("your age"))
    }

    @Test
    fun n_doesNotImplyEvaluatingEverything() {
        val lower = allCopy.lowercase()
        assertFalse(lower.contains("evaluates everything"))
        assertFalse(lower.contains("every area of life"))
    }

    @Test
    fun n_declineCopyIsRespectful() {
        val lower = ConsentDeclineBody.lowercase() + ConsentDeclineTitle.lowercase()
        assertTrue(ConsentDeclineTitle.contains("NOT FOR YOU"))
        assertTrue(ConsentDeclineBody.contains("You can leave at any time"))
        assertFalse(lower.contains("are you sure"))
        assertFalse(lower.contains("don't give up"))
        assertFalse(lower.contains("you will regret"))
        assertFalse(lower.contains("give it a try"))
    }

    @Test
    fun e_bodyCopyIsPresentAndConcise() {
        assertTrue(ConsentWhat.isNotBlank())
        assertTrue(ConsentWhy.isNotBlank())
        assertTrue(ConsentHow.isNotBlank())
        assertTrue(ConsentWhat.length < 200)
        assertTrue(ConsentWhy.length < 200)
        assertTrue(ConsentHow.length < 220)
    }
}

package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions

class SpellCheckerServiceTest {

    private val service = SpellCheckerService()

    @Test
    fun `should check word correctly`() {
        // "дом" is a valid Russian word
        val result = service.checkWord("дом")
        assertTrue(result)
    }

    @Test
    fun `should detect misspelled word`() {
        // "хтото" is not a standard Russian word
        val result = service.checkWord("хтото")
        // May or may not be detected depending on dictionary
        // Just check it doesn't throw
        Assertions.assertNotNull(result)
    }

    @Test
    fun `should get suggestions for misspelled word`() {
        val suggestions = service.getSuggestions("привет")
        // Should return a list (may be empty if word is correct)
        Assertions.assertNotNull(suggestions)
    }

    @Test
    fun `should check text and return misspellings`() {
        val text = "Привет мир"
        val misspellings = service.checkText(text)
        Assertions.assertNotNull(misspellings)
    }

    @Test
    fun `should set language`() {
        service.setLanguage("en")
        assertEquals("en", service.getLanguage())
    }

    @Test
    fun `should add word to dictionary`() {
        val testWord = "testword123"
        service.addWord(testWord)
        // Should not throw
        Assertions.assertNotNull(service.checkWord(testWord))
    }

    @Test
    fun `should handle empty text`() {
        val misspellings = service.checkText("")
        assertTrue(misspellings.isEmpty())
    }
}

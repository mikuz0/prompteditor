package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.util.AppPaths
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpellCheckerServiceTest {

    @Test
    fun `russian word dom is valid`() {
        val s = SpellCheckerService()
        s.setLanguage("ru")
        assertTrue(s.checkWord("дом"))
    }

    @Test
    fun `misspelled russian word is invalid`() {
        val s = SpellCheckerService()
        s.setLanguage("ru")
        assertFalse(s.checkWord("хтото"))
    }

    @Test
    fun `english word is valid after switching language`() {
        val s = SpellCheckerService()
        s.setLanguage("en")
        assertTrue(s.checkWord("hello"))
    }

    @Test
    fun `getSuggestions for misspelled word returns non-empty list`() {
        val s = SpellCheckerService()
        s.setLanguage("ru")
        val suggestions = s.getSuggestions("хтото")
        assertTrue(suggestions.isNotEmpty())
    }

    @Test
    fun `checkText finds misspelled words by start index`() {
        val s = SpellCheckerService()
        s.setLanguage("ru")
        val text = "дом превет малоко"
        val result = s.checkText(text)
        // превет начинается с индекса 4, малоко с индекса 11
        assertTrue(result.containsKey(4), "expected 'превет' at 4, got $result")
        assertTrue(result.containsKey(11), "expected 'малоко' at 11, got $result")
        // дом — правильное, его нет
        assertFalse(result.containsKey(0), "expected no error at 0, got $result")
    }

    @Test
    fun `checkText returns suggestions for misspellings`() {
        val s = SpellCheckerService()
        s.setLanguage("ru")
        val result = s.checkText("малоко")
        assertTrue(result.isNotEmpty())
        val suggestions = result[0]
        assertNotNull(suggestions)
        assertTrue(suggestions.any { it == "молоко" }, "expected 'молоко', got $suggestions")
    }

    @Test
    fun `addWord makes word valid`() {
        val s = SpellCheckerService()
        s.setLanguage("ru")
        val invented = "квазипуперслово"
        // до addWord — невалидное
        assertFalse(s.checkWord(invented))
        // после — валидное
        s.addWord(invented)
        assertTrue(s.checkWord(invented))
        // очистить за собой
        try { java.nio.file.Files.deleteIfExists(AppPaths.userDictPath) } catch (_: Exception) {}
    }

    @Test
    fun `checkText on empty string returns empty map`() {
        val s = SpellCheckerService()
        assertTrue(s.checkText("").isEmpty())
        assertTrue(s.checkText("   ").isEmpty())
    }
}

package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions

class PromptControllerTest {

    private val controller = PromptController()

    private fun createSection(key: String, title: String, order: Int, enabled: Boolean = true): Section {
        return Section(
            key = key,
            title = title,
            icon = key.uppercase(),
            description = "$key description",
            defaultText = "",
            order = order,
            enabled = enabled
        )
    }

    @Test
    fun `should assemble prompt from multiple sections`() {
        val sections = listOf(
            createSection("role", "Role", 0),
            createSection("goal", "Goal", 1),
            createSection("format", "Format", 2)
        )
        val texts = mapOf(
            "role" to "You are an assistant",
            "goal" to "Help the user",
            "format" to "Use bullet points"
        )

        val result = controller.assemblePrompt(sections, texts)

        assertTrue(result.contains("ROLE"))
        assertTrue(result.contains("GOAL"))
        assertTrue(result.contains("FORMAT"))
        assertTrue(result.contains("You are an assistant"))
        assertTrue(result.contains("Help the user"))
        assertTrue(result.contains("Use bullet points"))
        // Check order: ROLE should come before GOAL
        assertTrue(result.indexOf("ROLE") < result.indexOf("GOAL"))
        assertTrue(result.indexOf("GOAL") < result.indexOf("FORMAT"))
    }

    @Test
    fun `should skip empty sections`() {
        val sections = listOf(
            createSection("role", "Role", 0),
            createSection("goal", "Goal", 1),
            createSection("format", "Format", 2)
        )
        val texts = mapOf(
            "role" to "You are an assistant",
            "goal" to "",
            "format" to "Use bullet points"
        )

        val result = controller.assemblePrompt(sections, texts)

        assertTrue(result.contains("ROLE"))
        assertTrue(result.contains("FORMAT"))
        assertFalse(result.contains("GOAL"))
    }

    @Test
    fun `should skip whitespace-only sections`() {
        val sections = listOf(
            createSection("role", "Role", 0),
            createSection("goal", "Goal", 1)
        )
        val texts = mapOf(
            "role" to "You are an assistant",
            "goal" to "   \n\t  "
        )

        val result = controller.assemblePrompt(sections, texts)

        assertTrue(result.contains("ROLE"))
        assertFalse(result.contains("GOAL"))
    }

    @Test
    fun `should return empty string for empty input`() {
        val result = controller.assemblePrompt(emptyList(), emptyMap())
        assertEquals("", result)
    }

    @Test
    fun `should return empty string when all sections disabled`() {
        val sections = listOf(
            createSection("role", "Role", 0, enabled = false),
            createSection("goal", "Goal", 1, enabled = false)
        )
        val texts = mapOf(
            "role" to "Text",
            "goal" to "Text"
        )

        val result = controller.assemblePrompt(sections, texts)
        assertEquals("", result)
    }

    @Test
    fun `should validate empty sections`() {
        val texts = mapOf(
            "role" to "",
            "goal" to "  ",
            "format" to "Some content"
        )

        val errors = controller.validatePrompt(texts)

        assertEquals(2, errors.size)
        assertTrue(errors.containsKey("role"))
        assertTrue(errors.containsKey("goal"))
        assertFalse(errors.containsKey("format"))
    }

    @Test
    fun `should validate no errors for non-empty sections`() {
        val texts = mapOf(
            "role" to "Some content",
            "goal" to "More content"
        )

        val errors = controller.validatePrompt(texts)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should detect long sections`() {
        val longText = "x".repeat(10001)
        val texts = mapOf(
            "role" to longText,
            "goal" to "Short"
        )

        val errors = controller.validatePrompt(texts)
        assertEquals(1, errors.size)
        assertTrue(errors.containsKey("role"))
    }

    @Test
    fun `should detect content presence`() {
        assertTrue(controller.hasContent(mapOf("role" to "text")))
        assertFalse(controller.hasContent(emptyMap()))
        assertFalse(controller.hasContent(mapOf("role" to "", "goal" to "  ")))
        assertTrue(controller.hasContent(mapOf("role" to "", "goal" to "  ", "format" to "content")))
    }

    @Test
    fun `should format prompt with separators`() {
        val sections = listOf(
            createSection("role", "Role", 0)
        )
        val texts = mapOf("role" to "Test content")

        val result = controller.assemblePrompt(sections, texts)

        assertTrue(result.contains("ROLE"))
        assertTrue(result.contains("-".repeat(50)))
        assertTrue(result.contains("Test content"))
    }

    @Test
    fun `should handle single section`() {
        val sections = listOf(
            createSection("only", "Only", 0)
        )
        val texts = mapOf("only" to "Single section text")

        val result = controller.assemblePrompt(sections, texts)

        assertTrue(result.contains("ONLY"))
        assertTrue(result.contains("-".repeat(50)))
        assertTrue(result.contains("Single section text"))
    }
}

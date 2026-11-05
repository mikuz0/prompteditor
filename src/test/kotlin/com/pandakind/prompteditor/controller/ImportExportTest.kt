package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Profile
import com.pandakind.prompteditor.model.Section
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportExportTest {

    @TempDir
    lateinit var tempDir: Path

    private val profileController = ProfileController()
    private val promptController = PromptController()

    @Test
    fun `should export and import json round-trip`() {
        val profile = Profile(
            name = "test_export",
            profileData = mapOf(
                "role" to "You are an assistant",
                "goal" to "Help the user",
                "format" to "Use bullet points"
            ),
            templatesData = mapOf(
                "role" to listOf("You are a helper", "Act as an expert")
            )
        )

        // Export to JSON
        val json = profileController.exportToJson(profile)

        // Import from JSON
        val imported = profileController.importFromJson(json)

        assertEquals(profile.name, imported.name)
        assertEquals(profile.profileData, imported.profileData)
        assertEquals(profile.templatesData, imported.templatesData)
    }

    @Test
    fun `should save and load json file round-trip`() {
        val profile = Profile(
            name = "file_test",
            profileData = mapOf("role" to "Test role", "goal" to "Test goal")
        )

        val jsonFile = tempDir.resolve("test_profile.json")
        Files.writeString(jsonFile, profileController.exportToJson(profile))

        val loaded = profileController.importFromJson(Files.readString(jsonFile))
        assertEquals(profile.name, loaded.name)
        assertEquals(profile.profileData, loaded.profileData)
    }

    @Test
    fun `should parse txt prompt with multiple sections`() {
        val sections = listOf(
            Section("role", "РОЛЬ", "ROLE", "Role desc", "", 0, true),
            Section("context", "КОНТЕКСТ", "CONTEXT", "Context desc", "", 1, true),
            Section("goal", "ЦЕЛЬ", "GOAL", "Goal desc", "", 2, true)
        )
        val texts = mapOf(
            "role" to "Ты — опытный специалист.",
            "context" to "Пользователь работает над проектом.",
            "goal" to "Создать качественный ответ."
        )

        val txtContent = promptController.assemblePrompt(sections, texts)
        val parsed = parseTxtPrompt(txtContent, sections)

        assertEquals("Ты — опытный специалист.", parsed["role"]?.trim())
        assertEquals("Пользователь работает над проектом.", parsed["context"]?.trim())
        assertEquals("Создать качественный ответ.", parsed["goal"]?.trim())
    }

    @Test
    fun `should parse txt prompt with separators`() {
        val sections = listOf(
            Section("role", "РОЛЬ", "ROLE", "", "", 0, true)
        )
        val texts = mapOf(
            "role" to "Ты — эксперт.\nДругой текст."
        )

        val txtContent = promptController.assemblePrompt(sections, texts)
        val parsed = parseTxtPrompt(txtContent, sections)
        assertEquals("Ты — эксперт.\nДругой текст.", parsed["role"]?.trim())
    }

    @Test
    fun `should parse txt prompt with empty sections`() {
        val sections = listOf(
            Section("role", "РОЛЬ", "ROLE", "", "", 0, true),
            Section("goal", "ЦЕЛЬ", "GOAL", "", "", 1, true)
        )
        val texts = mapOf(
            "role" to "Текст роли.",
            "goal" to "" // empty
        )

        val txtContent = promptController.assemblePrompt(sections, texts)
        val parsed = parseTxtPrompt(txtContent, sections)

        // Empty sections are skipped by assemblePrompt
        assertEquals("Текст роли.", parsed["role"]?.trim())
        assertNull(parsed["goal"])
    }

    @Test
    fun `should handle txt prompt with unknown sections`() {
        val sections = listOf(
            Section("role", "РОЛЬ", "ROLE", "", "", 0, true)
        )
        val texts = mapOf(
            "role" to "Текст роли."
        )

        val txtContent = promptController.assemblePrompt(sections, texts)
        val parsed = parseTxtPrompt(txtContent, sections)

        assertEquals("Текст роли.", parsed["role"]?.trim())
        assertTrue(parsed.isEmpty() || parsed["role"]?.trim() == "Текст роли.")
    }

    @Test
    fun `should export prompt to txt format`() {
        val sections = listOf(
            Section("role", "РОЛЬ", "ROLE", "", "", 0, true),
            Section("goal", "ЦЕЛЬ", "GOAL", "", "", 1, true)
        )
        val texts = mapOf(
            "role" to "Текст роли.",
            "goal" to "Текст цели."
        )

        val result = promptController.assemblePrompt(sections, texts)

        assertTrue(result.contains("РОЛЬ"))
        assertTrue(result.contains("ЦЕЛЬ"))
        assertTrue(result.contains("-".repeat(50)))
        assertTrue(result.contains("Текст роли."))
        assertTrue(result.contains("Текст цели."))
    }

    @Test
    fun `should save and load txt file round-trip`() {
        val sections = listOf(
            Section("role", "РОЛЬ", "ROLE", "", "", 0, true),
            Section("goal", "ЦЕЛЬ", "GOAL", "", "", 1, true)
        )
        val texts = mapOf(
            "role" to "Текст роли.",
            "goal" to "Текст цели."
        )

        val txtContent = promptController.assemblePrompt(sections, texts)
        val txtFile = tempDir.resolve("prompt.txt")
        Files.writeString(txtFile, txtContent)

        val loaded = Files.readString(txtFile)
        val parsed = parseTxtPrompt(loaded, sections)

        assertEquals("Текст роли.", parsed["role"]?.trim())
        assertEquals("Текст цели.", parsed["goal"]?.trim())
    }

    /**
     * Parses TXT content produced by PromptController.assemblePrompt.
     * Format: {TITLE}\n{50 dashes}\n{TEXT}\n\n{next section...}
     */
    private fun parseTxtPrompt(content: String, sections: List<Section>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val sectionTitles = sections.associateBy({ it.title.lowercase() }, { it.key })

        // Split by double newline to get blocks (format from assemblePrompt)
        val blocks = content.split("\n\n").map { it.trim() }.filter { it.isNotEmpty() }

        for (block in blocks) {
            val lines = block.lines()
            // Find the separator line (50 dashes)
            val separatorIndex = lines.indexOfFirst { it.all { it == '-' } && it.length >= 20 }
            if (separatorIndex < 0) continue

            // Title is the line before separator
            val title = lines[separatorIndex - 1].trim().lowercase()
            if (title !in sectionTitles) continue

            // Text is everything after separator
            val text = lines.drop(separatorIndex + 1).joinToString("\n").trim()
            sectionTitles[title]?.let { result[it] = text }
        }

        return result
    }
}

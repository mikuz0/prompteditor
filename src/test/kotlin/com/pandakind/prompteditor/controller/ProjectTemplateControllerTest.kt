package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Section
import com.pandakind.prompteditor.model.Template
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectTemplateControllerTest {

    @TempDir
    lateinit var tempDir: Path

    private val controller = ProjectTemplateController()

    private fun sampleTemplate(name: String = "test"): Template = Template(
        name = name,
        sections = listOf(
            Section("role", "Роль", "ROLE", "desc", "default", 0, true),
            Section("goal", "Цель", "GOAL", "desc", "default", 1, true)
        ),
        templates = LinkedHashMap<String, List<String>>().apply {
            put("role", listOf("Ты эксперт"))
            put("goal", listOf("Сделай задачу"))
        }
    )

    @Test
    fun `save and load round-trip preserves sections and templates`() {
        val t = sampleTemplate()
        controller.save(tempDir, t)

        val loaded = controller.load(tempDir, "test")
        assertNotNull(loaded)
        assertEquals("test", loaded.name)
        assertEquals(2, loaded.sections.size)
        assertEquals("role", loaded.sections[0].key)
        assertEquals(listOf("Ты эксперт"), loaded.templates["role"])
        assertEquals(listOf("Сделай задачу"), loaded.templates["goal"])
    }

    @Test
    fun `list returns names without json sorted`() {
        controller.save(tempDir, sampleTemplate("beta"))
        controller.save(tempDir, sampleTemplate("alpha"))
        controller.save(tempDir, sampleTemplate("gamma"))

        assertEquals(listOf("alpha", "beta", "gamma"), controller.list(tempDir))
    }

    @Test
    fun `list on empty dir returns empty`() {
        assertTrue(controller.list(tempDir).isEmpty())
    }

    @Test
    fun `load nonexistent returns null`() {
        assertNull(controller.load(tempDir, "missing"))
    }

    @Test
    fun `delete removes file and returns true`() {
        controller.save(tempDir, sampleTemplate("to_delete"))
        assertTrue(controller.exists(tempDir, "to_delete"))

        assertTrue(controller.delete(tempDir, "to_delete"))
        assertFalse(controller.exists(tempDir, "to_delete"))
        assertTrue(controller.list(tempDir).isEmpty())
    }

    @Test
    fun `delete nonexistent returns false`() {
        assertFalse(controller.delete(tempDir, "missing"))
    }

    @Test
    fun `exists returns true for saved template`() {
        controller.save(tempDir, sampleTemplate("here"))
        assertTrue(controller.exists(tempDir, "here"))
    }

    @Test
    fun `exists returns false for unsaved template`() {
        assertFalse(controller.exists(tempDir, "nope"))
    }

    @Test
    fun `save creates templates directory inside project`() {
        controller.save(tempDir, sampleTemplate())
        assertTrue(Files.isDirectory(tempDir.resolve("templates")))
        assertTrue(Files.exists(tempDir.resolve("templates/test.json")))
    }
}

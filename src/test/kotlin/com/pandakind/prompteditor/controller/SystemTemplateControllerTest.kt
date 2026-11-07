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

class SystemTemplateControllerTest {

    @TempDir
    lateinit var tempDir: Path

    private fun controller() = SystemTemplateController(tempDir)

    private fun sampleTemplate(name: String = "system_test"): Template = Template(
        name = name,
        sections = listOf(
            Section("role", "Роль", "ROLE", "desc", "default", 0, true)
        ),
        templates = LinkedHashMap<String, List<String>>().apply {
            put("role", listOf("Ты эксперт"))
        }
    )

    @Test
    fun `save and load round-trip`() {
        val c = controller()
        c.save(sampleTemplate())

        val loaded = c.load("system_test")
        assertNotNull(loaded)
        assertEquals("system_test", loaded.name)
        assertEquals(1, loaded.sections.size)
        assertEquals(listOf("Ты эксперт"), loaded.templates["role"])
    }

    @Test
    fun `list returns names sorted`() {
        val c = controller()
        c.save(sampleTemplate("beta"))
        c.save(sampleTemplate("alpha"))
        c.save(sampleTemplate("gamma"))

        assertEquals(listOf("alpha", "beta", "gamma"), c.list())
    }

    @Test
    fun `list on empty dir returns empty`() {
        assertTrue(controller().list().isEmpty())
    }

    @Test
    fun `load nonexistent returns null`() {
        assertNull(controller().load("missing"))
    }

    @Test
    fun `delete removes file and returns true`() {
        val c = controller()
        c.save(sampleTemplate("to_delete"))
        assertTrue(c.delete("to_delete"))
        assertFalse(c.exists("to_delete"))
        assertTrue(c.list().isEmpty())
    }

    @Test
    fun `delete nonexistent returns false`() {
        assertFalse(controller().delete("missing"))
    }

    @Test
    fun `exists returns true for saved`() {
        val c = controller()
        c.save(sampleTemplate("here"))
        assertTrue(c.exists("here"))
    }

    @Test
    fun `exists returns false for unsaved`() {
        assertFalse(controller().exists("nope"))
    }

    @Test
    fun `save creates base directory`() {
        val sub = tempDir.resolve("nested/system")
        val c = SystemTemplateController(sub)
        c.save(sampleTemplate("x"))
        assertTrue(Files.isDirectory(sub))
        assertTrue(Files.exists(sub.resolve("x.json")))
    }
}

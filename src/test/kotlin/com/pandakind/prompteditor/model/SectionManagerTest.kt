package com.pandakind.prompteditor.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SectionManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private fun createTestConfig(content: String): Path {
        val configFile = tempDir.resolve("sections_config.json")
        configFile.toFile().writeText(content)
        return configFile
    }

    @Test
    fun `should load default config and sort by order`() {
        val configContent = """
            [
              {"key":"goal","title":"Goal","icon":"GOAL","description":"Goal desc","defaultText":"Default goal", "order":2, "enabled":true},
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true},
              {"key":"format","title":"Format","icon":"FORMAT","description":"Format desc","defaultText":"Default format", "order":1, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        val enabled = manager.getEnabledSections()
        assertEquals(3, enabled.size)
        assertEquals("role", enabled[0].key)
        assertEquals("format", enabled[1].key)
        assertEquals("goal", enabled[2].key)
    }

    @Test
    fun `should filter out disabled sections`() {
        val configContent = """
            [
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true},
              {"key":"context","title":"Context","icon":"CONTEXT","description":"Context desc","defaultText":"Default context", "order":1, "enabled":false},
              {"key":"goal","title":"Goal","icon":"GOAL","description":"Goal desc","defaultText":"Default goal", "order":2, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        val enabled = manager.getEnabledSections()
        assertEquals(2, enabled.size)
        assertEquals("role", enabled[0].key)
        assertEquals("goal", enabled[1].key)

        val all = manager.getAllSections()
        assertEquals(3, all.size)
    }

    @Test
    fun `should add new section`() {
        val configContent = """
            [
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        val newSection = Section(
            key = "goal",
            title = "Goal",
            icon = "GOAL",
            description = "Goal desc",
            defaultText = "Default goal",
            order = 1,
            enabled = true
        )
        manager.addSection(newSection)

        val all = manager.getAllSections()
        assertEquals(2, all.size)
        assertEquals("goal", manager.getSectionByKey("goal")?.key)
    }

    @Test
    fun `should throw on duplicate section key`() {
        val configContent = """
            [
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        val duplicateSection = Section(
            key = "role",
            title = "Role 2",
            icon = "ROLE2",
            description = "Duplicate",
            defaultText = "Duplicate text",
            order = 1,
            enabled = true
        )

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            manager.addSection(duplicateSection)
        }
    }

    @Test
    fun `should update existing section`() {
        val configContent = """
            [
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        val updated = manager.getSectionByKey("role")!!.copy(title = "Updated Role")
        manager.updateSection(updated)

        val section = manager.getSectionByKey("role")
        assertEquals("Updated Role", section?.title)
    }

    @Test
    fun `should remove section`() {
        val configContent = """
            [
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true},
              {"key":"goal","title":"Goal","icon":"GOAL","description":"Goal desc","defaultText":"Default goal", "order":1, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        manager.removeSection("role")

        val all = manager.getAllSections()
        assertEquals(1, all.size)
        assertEquals("goal", all[0].key)
        assertTrue(manager.getSectionByKey("role") == null)
    }

    @Test
    fun `should toggle section enabled state`() {
        val configContent = """
            [
              {"key":"role","title":"Role","icon":"ROLE","description":"Role desc","defaultText":"Default role", "order":0, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        manager.toggleEnabled("role", false)

        val enabled = manager.getEnabledSections()
        assertTrue(enabled.isEmpty())
        assertEquals(false, manager.getSectionByKey("role")?.enabled)
    }

    @Test
    fun `should move section up`() {
        val configContent = """
            [
              {"key":"a","title":"A","icon":"A","description":"A","defaultText":"A", "order":0, "enabled":true},
              {"key":"b","title":"B","icon":"B","description":"B","defaultText":"B", "order":1, "enabled":true},
              {"key":"c","title":"C","icon":"C","description":"C","defaultText":"C", "order":2, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        manager.moveSectionUp("c")

        val enabled = manager.getEnabledSections()
        assertEquals("a", enabled[0].key)
        assertEquals("c", enabled[1].key)
        assertEquals("b", enabled[2].key)
    }

    @Test
    fun `should move section down`() {
        val configContent = """
            [
              {"key":"a","title":"A","icon":"A","description":"A","defaultText":"A", "order":0, "enabled":true},
              {"key":"b","title":"B","icon":"B","description":"B","defaultText":"B", "order":1, "enabled":true},
              {"key":"c","title":"C","icon":"C","description":"C","defaultText":"C", "order":2, "enabled":true}
            ]
        """.trimIndent()

        val configFile = createTestConfig(configContent)
        val manager = SectionManager(configFile)

        manager.moveSectionDown("a")

        val enabled = manager.getEnabledSections()
        assertEquals("b", enabled[0].key)
        assertEquals("a", enabled[1].key)
        assertEquals("c", enabled[2].key)
    }

    @Test
    fun `should handle empty config`() {
        val configFile = createTestConfig("[]")
        val manager = SectionManager(configFile)

        assertTrue(manager.getEnabledSections().isEmpty())
        assertTrue(manager.getAllSections().isEmpty())
    }
}

package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Section
import com.pandakind.prompteditor.model.Template
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TemplateRoundTripTest {

    @TempDir
    lateinit var tempDir: Path

    private val mapper = ObjectMapper().apply { registerModule(kotlinModule()) }

    @Test
    fun `template json round-trip preserves sections and templates`() {
        val template = Template(
            name = "test",
            sections = listOf(
                Section("role", "Роль", "ROLE", "desc", "default", 0, true),
                Section("goal", "Цель", "GOAL", "desc", "default", 1, true)
            ),
            templates = LinkedHashMap<String, List<String>>().apply {
                put("role", listOf("Ты эксперт"))
                put("goal", listOf("Сделай задачу"))
            }
        )

        val file = tempDir.resolve("test.json")
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), template)

        val loaded = mapper.readValue(file.toFile(), Template::class.java)

        assertNotNull(loaded)
        assertEquals("test", loaded.name)
        assertEquals(2, loaded.sections.size)
        assertEquals("role", loaded.sections[0].key)
        assertEquals("Роль", loaded.sections[0].title)
        assertEquals(listOf("Ты эксперт"), loaded.templates["role"])
        assertEquals(listOf("Сделай задачу"), loaded.templates["goal"])
    }
}

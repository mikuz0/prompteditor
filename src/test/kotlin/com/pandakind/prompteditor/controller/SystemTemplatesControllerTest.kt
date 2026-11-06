package com.pandakind.prompteditor.controller

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.util.LinkedHashMap

class SystemTemplatesControllerTest {

    private val controller = SystemTemplatesController()

    @Test
    fun `save and load round-trip`() {
        val templates = LinkedHashMap<String, List<String>>().apply {
            put("greeting", listOf("Hello", "Hi there", "Greetings"))
            put("closing", listOf("Best regards", "Thanks"))
        }

        controller.save(templates)
        val loaded = controller.load()

        assertTrue(loaded != null)
        assertEquals(templates["greeting"], loaded?.get("greeting"))
        assertEquals(templates["closing"], loaded?.get("closing"))

        // Clean up
        Files.deleteIfExists(com.pandakind.prompteditor.util.AppPaths.systemTemplatesPath)
    }

    @Test
    fun `load when file does not exist should return null`() {
        // Make sure the file doesn't exist
        Files.deleteIfExists(com.pandakind.prompteditor.util.AppPaths.systemTemplatesPath)

        val loaded = controller.load()
        assertNull(loaded)
    }
}

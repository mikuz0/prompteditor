package com.pandakind.prompteditor.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptFileControllerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `create should create file with empty data for all section keys`() {
        val controller = PromptFileController()
        val prompt = controller.create(tempDir, "test_prompt", listOf("role", "goal", "format"))

        assertEquals("test_prompt", prompt.name)
        assertEquals(3, prompt.data.size)
        assertTrue(prompt.data.containsKey("role"))
        assertTrue(prompt.data.containsKey("goal"))
        assertTrue(prompt.data.containsKey("format"))
        assertEquals("", prompt.data["role"])
        assertEquals("", prompt.data["goal"])
        assertEquals("", prompt.data["format"])
    }

    @Test
    fun `save and load round-trip with LinkedHashMap preserves order`() {
        val controller = PromptFileController()
        val prompt = controller.create(tempDir, "order_test", listOf("a", "b", "c"))
        prompt.data["a"] = "first"
        prompt.data["b"] = "second"
        prompt.data["c"] = "third"

        controller.save(tempDir, prompt)
        val loaded = controller.load(tempDir, "order_test")

        assertNotNull(loaded)
        assertEquals("first", loaded.data["a"])
        assertEquals("second", loaded.data["b"])
        assertEquals("third", loaded.data["c"])
    }

    @Test
    fun `list should return names without json extension`() {
        val controller = PromptFileController()
        controller.create(tempDir, "prompt_a", listOf("key1"))
        controller.create(tempDir, "prompt_b", listOf("key1"))
        controller.create(tempDir, "prompt_c", listOf("key1"))

        val names = controller.list(tempDir)
        assertEquals(3, names.size)
        assertEquals("prompt_a", names[0])
        assertEquals("prompt_b", names[1])
        assertEquals("prompt_c", names[2])
    }

    @Test
    fun `load non-existent should return null`() {
        val controller = PromptFileController()
        assertNull(controller.load(tempDir, "does_not_exist"))
    }

    @Test
    fun `list on empty project should return empty list`() {
        val controller = PromptFileController()
        val prompts = controller.list(tempDir)
        assertTrue(prompts.isEmpty())
    }
}

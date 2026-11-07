package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Section
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectControllerTest {

    @TempDir
    lateinit var tempDir: Path

    private fun createDefaultSections(): List<Section> {
        return listOf(
            Section("role", "Role", "ROLE", "Role desc", "", 0, true),
                      Section("goal", "Goal", "GOAL", "Goal desc", "", 1, true)
        )
    }

    @Test
    fun `create should create project json templates default json and prompts directory`() {
        val projectDir = tempDir.resolve("MyProject")
        val controller = ProjectController()
        val project = controller.create(projectDir, createDefaultSections())

        assertEquals("MyProject", project.name)
        assertNull(project.lastPrompt)
        assertEquals("default", project.activeTemplate)
        assertTrue(Files.exists(projectDir.resolve("project.json")))
        assertTrue(Files.exists(projectDir.resolve("templates/default.json")))
        assertTrue(Files.isDirectory(projectDir.resolve("prompts")))
    }

    @Test
    fun `create should save default template with sections`() {
        val projectDir = tempDir.resolve("test_project")
        val controller = ProjectController()
        controller.create(projectDir, createDefaultSections())

        val templateFile = projectDir.resolve("templates/default.json")
        assertTrue(Files.exists(templateFile))
        val content = Files.readString(templateFile)
        assertTrue(content.contains("role"))
        assertTrue(content.contains("goal"))
    }

    @Test
    fun `open should read project json`() {
        val projectDir = tempDir.resolve("open_test")
        Files.createDirectories(projectDir.resolve("prompts"))
        val controller = ProjectController()

        val project = ProjectController().create(projectDir, createDefaultSections())
        val loaded = controller.open(projectDir)

        assertNotNull(loaded)
        assertEquals(project.name, loaded.name)
    }

    @Test
    fun `open should return null if project json missing`() {
        val projectDir = tempDir.resolve("no_project")
        Files.createDirectories(projectDir)
        val controller = ProjectController()
        val loaded = controller.open(projectDir)
        assertNull(loaded)
    }

    @Test
    fun `save should persist changes to project json`() {
        val projectDir = tempDir.resolve("save_test")
        val controller = ProjectController()
        val project = controller.create(projectDir, createDefaultSections())

        val updated = project.copy(lastPrompt = "prompt1")
        controller.save(updated, projectDir)

        val loaded = controller.open(projectDir)
        assertEquals("prompt1", loaded?.lastPrompt)
    }

    @Test
    fun `lastProjectPath should be saved and read`() {
        val controller = ProjectController()
        val testPath = tempDir.resolve("last_project_test")
        Files.createDirectories(testPath)

        controller.setLastProjectPath(testPath)
        val readPath = controller.getLastProjectPath()

        assertNotNull(readPath)
        assertEquals(testPath, readPath)
    }

    @Test
    fun `lastProjectPath should return null when not set`() {
        Files.deleteIfExists(com.pandakind.prompteditor.util.AppPaths.lastProjectPath)
        val controller = ProjectController()
        assertNull(controller.getLastProjectPath())
    }
}

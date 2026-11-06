package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Project
import com.pandakind.prompteditor.model.Section
import com.pandakind.prompteditor.util.AppPaths
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashMap

/**
 * Manages project lifecycle: create, open, save, close.
 * Projects are directories on disk containing project.json, sections.json, and prompts/.
 */
class ProjectController {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    /**
     * Create a new project in the specified directory.
     * Creates: project.json, sections.json (copy from default), prompts/ (empty).
     */
    fun create(projectDir: Path, defaultSections: List<Section>): Project {
        Files.createDirectories(projectDir)
        Files.createDirectories(projectDir.resolve("prompts"))

        // Copy default sections to project
        val sectionsFile = projectDir.resolve("sections.json")
        if (Files.exists(sectionsFile)) {
            // If sections.json already exists, don't overwrite
        } else {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(sectionsFile.toFile(), defaultSections)
        }

        val project = Project(name = projectDir.fileName.toString(), lastPrompt = null)
        save(project, projectDir)

        return project
    }

    /**
     * Open an existing project from its directory.
     * Returns null if project.json is missing or corrupted.
     */
    fun open(projectDir: Path): Project? {
        val projectFile = projectDir.resolve("project.json")
        return if (Files.exists(projectFile)) {
            try {
                objectMapper.readValue(projectFile.toFile(), Project::class.java)
            } catch (_: IOException) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Save project metadata to project.json in the project directory.
     */
    fun save(project: Project, projectDir: Path) {
        val projectFile = projectDir.resolve("project.json")
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(projectFile.toFile(), project)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save project to $projectFile", e)
        }
    }

    /**
     * Get the path to the last opened project file.
     */
    fun getLastProjectPath(): Path? {
        return try {
            if (Files.exists(AppPaths.lastProjectPath)) {
                Files.readString(AppPaths.lastProjectPath).takeIf { it.isNotBlank() }
                    ?.let { Path.of(it) }
            } else {
                null
            }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Save the path of the last opened project.
     */
    fun setLastProjectPath(path: Path) {
        try {
            Files.writeString(AppPaths.lastProjectPath, path.toString())
        } catch (e: IOException) {
            throw RuntimeException("Failed to save last project path", e)
        }
    }
}

package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Project
import com.pandakind.prompteditor.model.Section
import com.pandakind.prompteditor.model.Template
import com.pandakind.prompteditor.util.AppPaths
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ProjectController(
    private val projectTemplateController: ProjectTemplateController = ProjectTemplateController()
) {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    fun create(projectDir: Path, defaultSections: List<Section>, projectName: String? = null): Project {
        Files.createDirectories(projectDir)
        Files.createDirectories(projectDir.resolve("prompts"))

        val defaultTemplate = Template(name = "default", sections = defaultSections)
        projectTemplateController.save(projectDir, defaultTemplate)

        val name = projectName ?: projectDir.fileName.toString()
        val project = Project(name = name, lastPrompt = null, activeTemplate = "default")
        save(project, projectDir)

        return project
    }

    fun rename(project: Project, newName: String, projectDir: Path) {
        val updated = project.copy(name = newName)
        save(updated, projectDir)
    }

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

    fun save(project: Project, projectDir: Path) {
        val projectFile = projectDir.resolve("project.json")
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(projectFile.toFile(), project)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save project to $projectFile", e)
        }
    }

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

    fun setLastProjectPath(path: Path) {
        try {
            Files.writeString(AppPaths.lastProjectPath, path.toString())
        } catch (e: IOException) {
            throw RuntimeException("Failed to save last project path", e)
        }
    }
}

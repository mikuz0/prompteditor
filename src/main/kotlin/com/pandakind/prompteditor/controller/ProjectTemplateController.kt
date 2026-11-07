package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Template
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Manages templates stored in <projectDir>/templates directory.
 * Each template is a named .json file with sections + formulations.
 */
class ProjectTemplateController {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    private fun templatesDir(projectDir: Path): Path = projectDir.resolve("templates")

    fun list(projectDir: Path): List<String> {
        val dir = templatesDir(projectDir)
        if (!Files.exists(dir)) return emptyList()
        return Files.list(dir).use { stream ->
            stream
                .filter { it.fileName.toString().endsWith(".json") }
                .map { it.fileName.toString().removeSuffix(".json") }
                .sorted()
                .toList()
        }
    }

    fun load(projectDir: Path, name: String): Template? {
        val file = templatesDir(projectDir).resolve("$name.json")
        if (!Files.exists(file)) return null
        return try {
            objectMapper.readValue(file.toFile(), Template::class.java)
        } catch (_: IOException) {
            null
        }
    }

    fun save(projectDir: Path, template: Template) {
        val dir = templatesDir(projectDir)
        Files.createDirectories(dir)
        val file = dir.resolve("${template.name}.json")
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), template)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save template '${template.name}' to $file", e)
        }
    }

    fun delete(projectDir: Path, name: String): Boolean {
        val file = templatesDir(projectDir).resolve("$name.json")
        return try {
            Files.deleteIfExists(file)
        } catch (_: IOException) {
            false
        }
    }

    fun exists(projectDir: Path, name: String): Boolean {
        return Files.exists(templatesDir(projectDir).resolve("$name.json"))
    }
}
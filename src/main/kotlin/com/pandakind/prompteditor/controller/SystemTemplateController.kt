package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Template
import com.pandakind.prompteditor.util.AppPaths
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class SystemTemplateController(
    private val baseDir: Path = AppPaths.systemTemplatesDir
) {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    fun list(): List<String> {
        if (!Files.exists(baseDir)) return emptyList()
        return Files.list(baseDir).use { stream ->
            stream
                .filter { it.fileName.toString().endsWith(".json") }
                .map { it.fileName.toString().removeSuffix(".json") }
                .sorted()
                .toList()
        }
    }

    fun load(name: String): Template? {
        val file = baseDir.resolve("$name.json")
        if (!Files.exists(file)) return null
        return try {
            objectMapper.readValue(file.toFile(), Template::class.java)
        } catch (_: IOException) {
            null
        }
    }

    fun save(template: Template) {
        Files.createDirectories(baseDir)
        val file = baseDir.resolve("${template.name}.json")
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(file.toFile(), template)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save system template '${template.name}' to $file", e)
        }
    }

    fun delete(name: String): Boolean {
        val file = baseDir.resolve("$name.json")
        return try {
            Files.deleteIfExists(file)
        } catch (_: IOException) {
            false
        }
    }

    fun exists(name: String): Boolean {
        return Files.exists(baseDir.resolve("$name.json"))
    }
}

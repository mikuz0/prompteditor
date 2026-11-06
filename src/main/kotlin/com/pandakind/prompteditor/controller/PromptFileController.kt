package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Prompt
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashMap

/**
 * Manages prompt files within a project directory.
 * Each prompt is stored as prompts/<name>.json.
 */
class PromptFileController {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    /**
     * Create a new prompt file with empty data for all section keys.
     */
    fun create(projectDir: Path, name: String, sectionKeys: List<String>): Prompt {
        val data = LinkedHashMap<String, String>()
        for (key in sectionKeys) {
            data[key] = ""
        }
        val prompt = Prompt(name = name, data = data)
        save(projectDir, prompt)
        return prompt
    }

    /**
     * Export prompt to JSON string.
     */
    fun saveAsString(prompt: Prompt): String {
        return objectMapper.writeValueAsString(prompt)
    }

    /**
     * Import prompt from JSON string.
     */
    fun loadFromJson(json: String): Prompt {
        return objectMapper.readValue(json, Prompt::class.java)
    }

    /**
     * Save a prompt to prompts/<name>.json in the project directory.
     */
    fun save(projectDir: Path, prompt: Prompt) {
        val promptFile = projectDir.resolve("prompts").resolve("${prompt.name}.json")
        try {
            Files.createDirectories(promptFile.parent)
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(promptFile.toFile(), prompt)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save prompt '${prompt.name}' to $promptFile", e)
        }
    }

    /**
     * Load a prompt by name. Returns null if not found.
     */
    fun load(projectDir: Path, name: String): Prompt? {
        val promptFile = projectDir.resolve("prompts").resolve("$name.json")
        return if (Files.exists(promptFile)) {
            try {
                objectMapper.readValue(promptFile.toFile(), Prompt::class.java)
            } catch (_: IOException) {
                null
            }
        } else {
            null
        }
    }

    /**
     * List all prompt names in the project (without .json extension).
     */
    fun list(projectDir: Path): List<String> {
        val promptsDir = projectDir.resolve("prompts")
        return if (Files.exists(promptsDir) && Files.isDirectory(promptsDir)) {
            promptsDir.toFile().listFiles { f -> f.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?.sorted()
                ?: emptyList()
        } else {
            emptyList()
        }
    }
}

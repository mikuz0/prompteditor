package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.util.AppPaths
import com.fasterxml.jackson.core.type.TypeReference
import java.io.IOException
import java.nio.file.Files
import java.util.LinkedHashMap

/**
 * Manages system-level templates stored in ~/.config/prompt_editor/system_templates.json.
 * These templates are available across all projects.
 */
class SystemTemplatesController {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    /**
     * Load system templates. Returns null if the file doesn't exist.
     */
    fun load(): LinkedHashMap<String, List<String>>? {
        return try {
            if (Files.exists(AppPaths.systemTemplatesPath)) {
                objectMapper.readValue(
                    AppPaths.systemTemplatesPath.toFile(),
                    object : TypeReference<LinkedHashMap<String, List<String>>>() {}
                )
            } else {
                null
            }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Save system templates to ~/.config/prompt_editor/system_templates.json.
     */
    fun save(templates: Map<String, List<String>>) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(AppPaths.systemTemplatesPath.toFile(), templates)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save system templates", e)
        }
    }
}

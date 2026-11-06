package com.pandakind.prompteditor.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.LinkedHashMap

/**
 * Project model: stored in project.json inside the project directory.
 * Contains name, last prompt name, and user-defined templates.
 */
data class Project(
    @JsonProperty("name") val name: String,
    @JsonProperty("lastPrompt") val lastPrompt: String? = null,
    @JsonProperty("templates") val templates: LinkedHashMap<String, List<String>> = LinkedHashMap()
)

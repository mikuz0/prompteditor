package com.pandakind.prompteditor.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.LinkedHashMap

/**
 * Prompt model: stored in prompts/<name>.json inside the project directory.
 * Contains name and a map of section-key → text.
 */
data class Prompt(
    @JsonProperty("name") val name: String,
    @JsonProperty("data") val data: LinkedHashMap<String, String> = LinkedHashMap()
)

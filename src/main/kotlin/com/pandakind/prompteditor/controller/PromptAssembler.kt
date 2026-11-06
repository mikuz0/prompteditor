package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.DisplaySection

/**
 * Assembles a formatted prompt string from display sections and their text data.
 * Includes both config sections (from sections.json) and implicit sections
 * (from templates/prompts that have no config entry).
 *
 * Format: {TITLE.uppercase()}\n{50 dashes}\n{text}
 * Empty sections are skipped.
 */
class PromptAssembler {

    fun assemblePrompt(displaySections: List<DisplaySection>, data: Map<String, String>): String {
        val parts = mutableListOf<String>()

        for (section in displaySections) {
            if (!section.enabled) continue
            val text = data[section.key]?.trim()
            if (text.isNullOrEmpty()) continue

            // Config sections use section.title.uppercase(), implicit sections use key.uppercase()
            val header = if (!section.isImplicit) {
                section.title.uppercase()
            } else {
                section.key.uppercase()
            }
            parts.add("$header\n${"-".repeat(50)}\n$text")
        }

        return parts.joinToString("\n\n")
    }

    fun validatePrompt(data: Map<String, String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        for ((key, text) in data) {
            if (text.trim().isEmpty()) {
                errors[key] = "Раздел '$key' пуст"
            } else if (text.length > 10000) {
                errors[key] = "Раздел '$key' слишком длинный (>10000 символов)"
            }
        }
        return errors
    }

    fun hasContent(data: Map<String, String>): Boolean =
        data.values.any { it.trim().isNotEmpty() }
}

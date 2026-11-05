package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Section

class PromptController {

    fun assemblePrompt(sections: List<Section>, sectionTexts: Map<String, String>): String {
        val parts = mutableListOf<String>()

        for (section in sections.filter { it.enabled }.sortedBy { it.order }) {
            val text = sectionTexts[section.key]?.trim()
            if (text.isNullOrEmpty()) continue
            parts.add("${section.title.uppercase()}\n${"-".repeat(50)}\n$text")
        }

        return parts.joinToString("\n\n")
    }

    fun validatePrompt(sectionTexts: Map<String, String>): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        for ((key, text) in sectionTexts) {
            if (text.trim().isEmpty()) {
                errors[key] = "Раздел '$key' пуст"
            } else if (text.length > 10000) {
                errors[key] = "Раздел '$key' слишком длинный (>10000 символов)"
            }
        }
        return errors
    }

    fun hasContent(sectionTexts: Map<String, String>): Boolean =
        sectionTexts.values.any { it.trim().isNotEmpty() }
}
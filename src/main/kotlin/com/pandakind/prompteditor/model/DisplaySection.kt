package com.pandakind.prompteditor.model

/**
 * UI-only section representation. Combines persisted Section data
 * with implicit flag for sections that exist only in templates/prompts.
 * Not written to sections.json.
 */
data class DisplaySection(
    val key: String,
    val title: String,
    val icon: String = "",
    val description: String = "",
    val defaultText: String = "",
    val order: Int = 0,
    val enabled: Boolean = true,
    val isImplicit: Boolean = false
) {
    companion object {
        /** Convert persisted Section to DisplaySection (not implicit). */
        fun fromSection(section: Section): DisplaySection =
            DisplaySection(
                key = section.key,
                title = section.title,
                icon = section.icon,
                description = section.description,
                defaultText = section.defaultText,
                order = section.order,
                enabled = section.enabled,
                isImplicit = false
            )
    }
}

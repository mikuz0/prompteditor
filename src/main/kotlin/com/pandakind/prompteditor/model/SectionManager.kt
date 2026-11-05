package com.pandakind.prompteditor.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class SectionManager(private val configPath: Path) {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    private var sections: MutableList<Section> = mutableListOf()

    init {
        load()
    }

    fun load() {
        try {
            if (Files.exists(configPath)) {
                sections = objectMapper.readValue(
                    configPath.toFile(),
                    object : TypeReference<List<Section>>() {}
                ).toMutableList()
            } else {
                sections = mutableListOf()
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to load sections config from $configPath", e)
        }
    }

    fun save() {
        try {
            Files.createDirectories(configPath.parent)
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(configPath.toFile(), sections)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save sections config to $configPath", e)
        }
    }

    fun getEnabledSections(): List<Section> =
        sections.filter { it.enabled }.sortedBy { it.order }

    fun getAllSections(): List<Section> = sections.toList()

    fun addSection(section: Section) {
        require(sections.none { it.key == section.key }) {
            "Section with key '${section.key}' already exists"
        }
        sections.add(section)
        save()
    }

    fun updateSection(section: Section) {
        val index = sections.indexOfFirst { it.key == section.key }
        require(index != -1) { "Section with key '${section.key}' not found" }
        sections[index] = section
        save()
    }

    fun removeSection(key: String) {
        val removed = sections.removeAll { it.key == key }
        require(removed) { "Section with key '$key' not found" }
        save()
    }

    /** Меняет порядок в списке: [a, b, c] moveUp("c") -> [a, c, b]. */
    fun moveSectionUp(key: String) {
        val enabled = getEnabledSections()
        val idx = enabled.indexOfFirst { it.key == key }
        require(idx > 0) { "Section '$key' is already at the top" }
        swapOrder(enabled[idx], enabled[idx - 1])
    }

    fun moveSectionDown(key: String) {
        val enabled = getEnabledSections()
        val idx = enabled.indexOfFirst { it.key == key }
        require(idx in 0 until enabled.size - 1) { "Section '$key' is already at the bottom" }
        swapOrder(enabled[idx], enabled[idx + 1])
    }

    /** Один атомарный save() после свопа двух order-значений. */
    private fun swapOrder(a: Section, b: Section) {
        val ia = sections.indexOfFirst { it.key == a.key }
        val ib = sections.indexOfFirst { it.key == b.key }
        if (ia == -1 || ib == -1) return

        val oa = sections[ia].order
        val ob = sections[ib].order
        sections[ia] = sections[ia].copy(order = ob)
        sections[ib] = sections[ib].copy(order = oa)
        save()
    }

    fun toggleEnabled(key: String, enabled: Boolean) {
        val index = sections.indexOfFirst { it.key == key }
        require(index != -1) { "Section with key '$key' not found" }
        sections[index] = sections[index].copy(enabled = enabled)
        save()
    }

    fun getSectionByKey(key: String): Section? = sections.find { it.key == key }

    fun getSectionByTitle(title: String): Section? = sections.find { it.title == title }
}
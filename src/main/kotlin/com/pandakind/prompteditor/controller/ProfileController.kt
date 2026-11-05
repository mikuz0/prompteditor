package com.pandakind.prompteditor.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.pandakind.prompteditor.model.Profile
import com.pandakind.prompteditor.util.AppPaths
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class ProfileController(private val customProfilesDir: Path? = null) {

    private val objectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
    }

    private val profilesDir: Path = customProfilesDir ?: AppPaths.profilesDir
    private val lastProfilePath: Path = customProfilesDir?.resolve("last_profile.txt") ?: AppPaths.lastProfilePath

    init {
        Files.createDirectories(profilesDir)
        Files.createDirectories(lastProfilePath.parent)
    }

    fun saveProfile(profile: Profile) {
        val file = profilesDir.resolve("${profile.name}.json")
        try {
            objectMapper.writeValue(file.toFile(), profile)
            saveLastProfile(profile.name)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save profile '${profile.name}'", e)
        }
    }

    fun saveProfileAs(profile: Profile, newName: String) {
        val newProfile = profile.copy(name = newName)
        saveProfile(newProfile)
    }

    fun loadProfile(name: String): Profile? {
        val file = profilesDir.resolve("$name.json")
        return if (Files.exists(file)) {
            try {
                objectMapper.readValue(file.toFile(), Profile::class.java)
            } catch (e: IOException) {
                null
            }
        } else {
            null
        }
    }

    fun listProfiles(): List<String> {
        return profilesDir.toFile().listFiles { f -> f.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun deleteProfile(name: String): Boolean {
        val file = profilesDir.resolve("$name.json")
        return if (Files.exists(file)) {
            file.toFile().delete()
            val currentLast = loadLastProfile()
            if (currentLast == name) {
                Files.deleteIfExists(lastProfilePath)
            }
            true
        } else {
            false
        }
    }

    fun saveLastProfile(name: String) {
        try {
            Files.writeString(lastProfilePath, name)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save last profile name", e)
        }
    }

    fun loadLastProfile(): String? {
        return try {
            if (Files.exists(lastProfilePath)) {
                Files.readString(lastProfilePath).takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    fun exportToJson(profile: Profile): String {
        return objectMapper.writeValueAsString(profile)
    }

    fun importFromJson(json: String): Profile {
        return objectMapper.readValue(json, Profile::class.java)
    }
}

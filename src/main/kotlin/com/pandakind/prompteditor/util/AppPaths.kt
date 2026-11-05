package com.pandakind.prompteditor.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object AppPaths {

    private const val APP_DIR_NAME = "prompt_editor"

    val configDir: Path by lazy {
        val dir = resolveBaseDir().resolve(APP_DIR_NAME)
        Files.createDirectories(dir)
        dir
    }

    val profilesDir: Path by lazy {
        val dir = configDir.resolve("profiles")
        Files.createDirectories(dir)
        dir
    }

    val userDictPath: Path by lazy {
        val dir = configDir.resolve("user_dict")
        Files.createDirectories(dir)
        dir.resolve("user_dictionary.txt")
    }

    val lastProfilePath: Path by lazy {
        configDir.resolve("last_profile.txt")
    }

    /** Рабочая копия sections_config.json в пользовательской директории. */
    val sectionsConfigPath: Path by lazy {
        configDir.resolve("sections_config.json")
    }

    private fun resolveBaseDir(): Path {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")
                if (appData != null) Paths.get(appData) else xdgConfigDir()
            }
            os.contains("mac") -> {
                Paths.get(System.getProperty("user.home"), "Library", "Application Support")
            }
            else -> {
                val xdg = System.getenv("XDG_CONFIG_HOME")
                if (!xdg.isNullOrBlank()) Paths.get(xdg) else xdgConfigDir()
            }
        }
    }

    private fun xdgConfigDir(): Path =
        Paths.get(System.getProperty("user.home"), ".config")

    fun ensureDirectories() {
        Files.createDirectories(configDir)
        Files.createDirectories(profilesDir)
        Files.createDirectories(userDictPath.parent)
        Files.createDirectories(lastProfilePath.parent)
    }
}
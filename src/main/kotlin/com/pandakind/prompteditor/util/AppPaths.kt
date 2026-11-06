package com.pandakind.prompteditor.util

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object AppPaths {

    private const val APP_DIR_NAME = "prompt_editor"

    val configDir: Path = run {
        val dir = resolveBaseDir().resolve(APP_DIR_NAME)
        Files.createDirectories(dir)
        dir
    }

    /** Default sections config template (not modified by the app). */
    val defaultSectionsPath: Path by lazy {
        configDir.resolve("sections_config.json")
    }

    /** System-level templates, overwritten via "Save as system template". */
    val systemTemplatesPath: Path by lazy {
        configDir.resolve("system_templates.json")
    }

    /** Path to the last opened project directory (absolute path string). */
    val lastProjectPath: Path by lazy {
        configDir.resolve("last_project.txt")
    }

    /** User-configured workspace directory for projects. */
    val workspacePath: Path = configDir.resolve("workspace_path.txt")

    /** Get the workspace directory path. Returns null if not set. */
    fun resolveWorkspacePath(): Path? {
        return try {
            if (Files.exists(workspacePath)) {
                Files.readString(workspacePath).takeIf { it.isNotBlank() }
                    ?.let { Path.of(it) }
            } else null
        } catch (_: IOException) {
            null
        }
    }

    /** Save the workspace directory path. */
    fun setWorkspacePath(path: Path) {
        try {
            Files.writeString(workspacePath, path.toString())
        } catch (e: IOException) {
            throw RuntimeException("Failed to save workspace path", e)
        }
    }

    val userDictPath: Path by lazy {
        val dir = configDir.resolve("user_dict")
        Files.createDirectories(dir)
        dir.resolve("user_dictionary.txt")
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
        // configDir and workspacePath are already initialized eagerly
        Files.createDirectories(configDir)
        Files.createDirectories(userDictPath.parent)
    }
}

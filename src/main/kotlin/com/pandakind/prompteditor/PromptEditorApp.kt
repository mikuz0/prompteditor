package com.pandakind.prompteditor

import com.pandakind.prompteditor.controller.ProfileController
import com.pandakind.prompteditor.controller.PromptController
import com.pandakind.prompteditor.controller.SpellCheckerService
import com.pandakind.prompteditor.model.SectionManager
import com.pandakind.prompteditor.util.AppPaths
import com.pandakind.prompteditor.view.MainWindow
import javafx.application.Application
import javafx.stage.Stage
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PromptEditorApp : Application() {

    override fun init() {
        AppPaths.ensureDirectories()
        bootstrapSectionsConfig()
    }

    override fun start(primaryStage: Stage) {
        val sectionManager = SectionManager(AppPaths.sectionsConfigPath)
        val profileController = ProfileController()
        val promptController = PromptController()
        val spellChecker = SpellCheckerService()

        MainWindow(
            primaryStage,
            sectionManager,
            profileController,
            promptController,
            spellChecker
        ).show()
    }

    private fun bootstrapSectionsConfig() {
        val target = AppPaths.sectionsConfigPath
        if (Files.exists(target)) return
        val stream = javaClass.getResourceAsStream("/config/sections_config.json")
            ?: javaClass.getResourceAsStream("/sections_config.json")
            ?: return
        stream.use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

fun main() {
    Application.launch(PromptEditorApp::class.java)
}
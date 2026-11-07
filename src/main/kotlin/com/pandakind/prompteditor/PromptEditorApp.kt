package com.pandakind.prompteditor

import com.pandakind.prompteditor.controller.ProjectController
import com.pandakind.prompteditor.controller.PromptAssembler
import com.pandakind.prompteditor.controller.PromptFileController
import com.pandakind.prompteditor.controller.SpellCheckerService
import com.pandakind.prompteditor.controller.SystemTemplateController
import com.pandakind.prompteditor.model.*
import com.pandakind.prompteditor.util.AppPaths
import com.pandakind.prompteditor.view.MainWindow
import com.pandakind.prompteditor.view.TabEditorView
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.nio.file.Files
import java.util.LinkedHashMap

class PromptEditorApp : Application() {

    override fun init() {
        AppPaths.ensureDirectories()
        bootstrapSectionsConfig()
    }

    override fun start(primaryStage: Stage) {
        val projectController = ProjectController()
        val promptFileController = PromptFileController()
        val projectTemplateController = com.pandakind.prompteditor.controller.ProjectTemplateController()
        val systemTemplateController = SystemTemplateController()
        val promptAssembler = PromptAssembler()
        val spellChecker = SpellCheckerService()

        MainWindow(
            primaryStage,
            projectController,
            promptFileController,
            projectTemplateController,
            systemTemplateController,
            promptAssembler,
            spellChecker
        ).show()
    }

    private fun bootstrapSectionsConfig() {
        val target = AppPaths.defaultSectionsPath
        if (Files.exists(target)) return
        val stream = javaClass.getResourceAsStream("/config/sections_config.json")
            ?: javaClass.getResourceAsStream("/sections_config.json")
            ?: return
        stream.use { input ->
            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

fun main() {
    Application.launch(PromptEditorApp::class.java)
}

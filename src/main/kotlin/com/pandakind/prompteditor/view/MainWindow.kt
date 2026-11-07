package com.pandakind.prompteditor.view

import com.pandakind.prompteditor.controller.ProjectController
import com.pandakind.prompteditor.controller.PromptAssembler
import com.pandakind.prompteditor.controller.PromptFileController
import com.pandakind.prompteditor.controller.ProjectTemplateController
import com.pandakind.prompteditor.controller.SpellCheckerService
import com.pandakind.prompteditor.controller.SystemTemplateController
import com.pandakind.prompteditor.model.*
import com.pandakind.prompteditor.util.AppPaths
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedHashMap

class MainWindow(
    private val primaryStage: Stage,
    private val projectController: ProjectController,
    private val promptFileController: PromptFileController,
    private val projectTemplateController: ProjectTemplateController,
    private val systemTemplateController: SystemTemplateController,
    private val promptAssembler: PromptAssembler,
    private val spellChecker: SpellCheckerService
) {
    // Project state
    private var currentProject: Project? = null
    private var currentPromptName: String? = null
    private var projectDir: java.nio.file.Path? = null
    private var isDirty: Boolean = false

    // Active template
    private var activeTemplate: Template? = null
    private var activeTemplateName: String? = null

    // Per-project data
    private val tabEditors = mutableMapOf<String, TabEditorView>()
    private val sectionTexts = mutableMapOf<String, String>()

    // UI components
    private val projectLabel = Label("Проект не открыт")
    private val tabPane = TabPane()
    private val assemblyViewer = TextArea().apply { isEditable = false; isWrapText = true }

    init {
        primaryStage.title = "Prompt Editor v2.2"
        primaryStage.scene = buildScene()
    }

    fun show() {
        primaryStage.show()
        tryAutoOpenLastProject()
    }

    // ─── Scene Building ──────────────────────────────────────────────

    private fun buildScene(): Scene {
        val topPanel = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(10.0)
            style = "-fx-background-color: #f0f0f0;"
            children.addAll(
                projectLabel.apply { style = "-fx-font-size: 14px; -fx-font-weight: bold;" },
                Button("Проверить орфографию").apply { setOnAction { checkAllSpellcheck() } }
            )
        }

        val leftPanel = VBox(tabPane).apply {
            VBox.setVgrow(tabPane, Priority.ALWAYS)
            padding = Insets(10.0)
            style = "-fx-background-color: #fafafa;"
        }

        val assemblyPanel = buildAssemblyPanel()

        val split = SplitPane(leftPanel, assemblyPanel).apply {
            orientation = Orientation.HORIZONTAL
            setDividerPositions(0.6)
        }
        VBox.setVgrow(split, Priority.ALWAYS)

        val root = VBox(buildMenuBar(), topPanel, split)
        val scene = Scene(root, 1200.0, 800.0)
        javaClass.getResource("/app.css")?.let { scene.stylesheets.add(it.toExternalForm()) }
        return scene
    }

    private fun buildMenuBar(): MenuBar {
        // Project menu
        projectMenu = Menu("Проект").apply {
            items.addAll(
                item("Создать...") { createProject() },
                item("Открыть...") { openProject() },
                item("Настройки проекта...") { openProjectSettings() },
                SeparatorMenuItem(),
                item("Закрыть проект") { closeProject() },
                SeparatorMenuItem(),
                item("Выход") { primaryStage.close() }
            )
        }

        // Prompt menu
        promptMenu = Menu("Промт").apply {
            items.addAll(
                item("Создать новый...") { createNewPrompt() },
                item("Открыть...") { openPrompt() },
                SeparatorMenuItem(),
                item("Сохранить") { savePrompt() },
                item("Сохранить как...") { savePromptAs() },
                SeparatorMenuItem(),
                item("Экспорт в .txt...") { exportTxt() },
                item("Импорт из .txt...") { importTxt() }
            )
        }

        // Template menu
        templateMenu = Menu("Шаблон").apply {
            items.addAll(
                item("Открыть из проекта...") { openProjectTemplate() },
                item("Открыть системный...") { openSystemTemplate() },
                SeparatorMenuItem(),
                item("Сохранить") { saveActiveTemplate() },
                item("Сохранить как...") { saveActiveTemplateAs() },
                item("Сохранить как системный...") { saveActiveTemplateAsSystem() }
            )
        }

        // Settings menu
        settingsMenu = Menu("Настройки").apply {
            items.addAll(
                item("Редактор разделов...") { openSectionEditor() },
                SeparatorMenuItem(),
                item("Проверить все разделы") { checkAllSpellcheck() },
                SeparatorMenuItem(),
                item("Глобальные настройки...") { openGlobalSettings() }
            )
        }

        // Help menu
        val helpMenu = Menu("Справка").apply {
            items.add(item("О программе") { showAbout() })
        }

        return MenuBar(projectMenu, promptMenu, templateMenu, settingsMenu, helpMenu)
    }

    /** Update disable state for menu items that require a project. */
    private fun updateMenuDisables() {
        val hasProject = currentProject != null
        val hasPrompt = currentPromptName != null

        // Project menu: only "Создать...", "Открыть...", "Выход" always enabled
        val projectAlwaysEnabled = setOf("Создать...", "Открыть...", "Выход")
        projectMenu.items.forEach { item ->
            if (item is MenuItem && item.text !in projectAlwaysEnabled) {
                item.setDisable(!hasProject)
            }
        }

        // Prompt menu: items needing project vs needing prompt
        val promptNeedsPrompt = setOf("Сохранить")  // только "Сохранить" требует prompt
        val promptNeedsProject = setOf(
            "Создать новый...", "Открыть...", "Экспорт в .txt...", "Импорт из .txt...",
            "Сохранить как..."  // доступен при любом проекте
        )
        promptMenu.items.forEach { item ->
            if (item is MenuItem) {
                item.setDisable(when (item.text) {
                    in promptNeedsProject -> !hasProject
                    in promptNeedsPrompt -> !hasPrompt
                    else -> false
                })
            }
        }

        // Template menu: always needs project
        templateMenu.items.forEach { item ->
            if (item is MenuItem) item.setDisable(!hasProject)
        }

        // Settings menu: "Проверить все разделы" always enabled
        settingsMenu.items.forEach { item ->
            if (item is MenuItem && item.text != "Проверить все разделы") {
                item.setDisable(!hasProject)
            }
        }
    }

    private lateinit var projectMenu: Menu
    private lateinit var promptMenu: Menu
    private lateinit var templateMenu: Menu
    private lateinit var settingsMenu: Menu

    private fun item(text: String, action: () -> Unit): MenuItem =
        MenuItem(text).apply { setOnAction { action() } }

    private fun buildAssemblyPanel(): VBox {
        val buttons = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(5.0)
            children.addAll(
                Button("Собрать промт").apply { setOnAction { assemblePrompt() } },
                Button("Копировать в буфер").apply { setOnAction { copyToClipboard() } },
                Button("Сохранить в файл").apply { setOnAction { saveToFile() } }
            )
        }
        val scroll = ScrollPane(assemblyViewer).apply {
            isFitToHeight = true
            isFitToWidth = true
        }
        VBox.setVgrow(scroll, Priority.ALWAYS)
        return VBox(10.0, buttons, scroll).apply {
            padding = Insets(10.0)
            style = "-fx-background-color: #fff;"
        }
    }

    // ─── Menu Actions ────────────────────────────────────────────────

    /** Create a new project by selecting a directory. */
    private fun createProject() {
        val chooser = DirectoryChooser().apply {
            title = "Создать проект (выберите папку)"
            initialDirectory = AppPaths.resolveWorkspacePath()?.toFile()
                ?: java.nio.file.Paths.get(System.getProperty("user.home")).toFile()
        }
        val dir = chooser.showDialog(primaryStage)?.toPath() ?: return

        val projectFile = dir.resolve("project.json")
        if (Files.exists(projectFile)) {
            val choice = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Проект существует"
                headerText = null
                contentText = "Проект уже существует в этой папке. Открыть его?"
                buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
            }.showAndWait().orElse(ButtonType.NO)
            if (choice == ButtonType.YES) openProjectAt(dir)
            return
        }

        val defaultSections = loadDefaultSections()
        val project = projectController.create(dir, defaultSections)
        openProjectAt(dir, project)
    }

    /** Open project settings dialog (currently only name). */
    private fun openProjectSettings() {
        if (currentProject == null || projectDir == null) return

        val dialog = TextInputDialog(currentProject!!.name).apply {
            title = "Настройки проекта"
            headerText = "Имя проекта"
            contentText = "Отображаемое имя:"
        }
        dialog.showAndWait().ifPresent { raw ->
            val name = raw.trim()
            if (name.isBlank()) {
                Alert(Alert.AlertType.ERROR, "Имя проекта не может быть пустым").show()
                return@ifPresent
            }
            projectController.rename(currentProject!!, name, projectDir!!)
            currentProject = currentProject!!.copy(name = name)
            projectLabel.text = "Проект: $name"
            updateWindowTitle()
        }
    }

    /** Open an existing project by selecting a directory. */
    private fun openProject() {
        val chooser = DirectoryChooser().apply {
            title = "Открыть проект"
            initialDirectory = AppPaths.resolveWorkspacePath()?.toFile()
                ?: java.nio.file.Paths.get(System.getProperty("user.home")).toFile()
        }
        val dir = chooser.showDialog(primaryStage)?.toPath() ?: return

        if (Files.exists(dir.resolve("project.json"))) {
            openProjectAt(dir)
        } else {
            val choice = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Проект не найден"
                headerText = null
                contentText = "В этой папке нет project.json. Создать новый проект?"
                buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
            }.showAndWait().orElse(ButtonType.NO)
            if (choice == ButtonType.YES) {
                val defaultSections = loadDefaultSections()
                val project = projectController.create(dir, defaultSections)
                openProjectAt(dir, project)
            }
        }
    }

    /** Open a project at the given directory path. */
    private fun openProjectAt(dir: java.nio.file.Path, project: Project? = null) {
        // Validate first — don't reset UI until we know the project opens
        val actualProject = project ?: projectController.open(dir)
        if (actualProject == null) {
            Alert(Alert.AlertType.ERROR, "Не удалось открыть проект из $dir").show()
            return
        }

        // Now ask to save old project if dirty
        if (currentProject != null && isDirty) {
            val choice = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Сохранить изменения?"
                headerText = "Есть несохранённые изменения"
                contentText = "Сохранить перед открытием другого проекта?"
                buttonTypes.setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
            }.showAndWait().orElse(ButtonType.CANCEL)
            when (choice) {
                ButtonType.YES -> saveCurrentPrompt()
                ButtonType.CANCEL -> return
                else -> { /* NO — discard unsaved */ }
            }
        }

        // Safe to reset now
        resetUI()

        currentProject = actualProject
        projectDir = dir
        currentPromptName = null
        isDirty = false
        updateMenuDisables()

        // Load active template
        val templateName = actualProject.activeTemplate ?: "default"
        var template = projectTemplateController.load(dir, templateName)
        if (template == null) {
            if (templateName == "default") {
                val defaultSections = loadDefaultSections()
                template = Template(name = "default", sections = defaultSections)
                projectTemplateController.save(dir, template)
                if (actualProject.activeTemplate != "default") {
                    val updated = actualProject.copy(activeTemplate = "default")
                    projectController.save(updated, dir)
                    currentProject = updated
                }
            } else {
                Alert(Alert.AlertType.WARNING,
                    "Шаблон '$templateName' не найден. Используем default.").show()
                val defaultSections = loadDefaultSections()
                template = Template(name = "default", sections = defaultSections)
                projectTemplateController.save(dir, template)
                activeTemplateName = "default"
            }
        }
        activeTemplate = template
        activeTemplateName = template.name

        // Update UI
        projectLabel.text = "Проект: ${actualProject.name}"
        primaryStage.title = actualProject.name

        // Build tabs from sections
        refreshSections()

        // Load last prompt if specified
        actualProject.lastPrompt?.let { lastPrompt ->
            val loaded = promptFileController.load(dir, lastPrompt)
            if (loaded != null) {
                currentPromptName = lastPrompt
                applyPromptData(loaded.data)
                isDirty = false
            }
        }

        // Save last project path
        projectController.setLastProjectPath(dir)
        // Update workspace path for DirectoryChooser
        AppPaths.setWorkspacePath(dir.parent ?: dir)
    }

    /** Close the current project. */
    private fun closeProject() {
        if (currentProject == null) return

        if (isDirty) {
            val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "Сохранить изменения?"
                headerText = "Есть несохранённые изменения"
                contentText = "Сохранить изменения перед закрытием?"
            }
            alert.showAndWait().let { result ->
                when (result.orElse(ButtonType.CANCEL)) {
                    ButtonType.OK -> saveCurrentPrompt()
                    ButtonType.CANCEL -> return
                    else -> {}
                }
            }
        }

        resetUI()
        projectLabel.text = "Проект не открыт"
        primaryStage.title = "Prompt Editor v2.2"
        updateMenuDisables()
    }

    /** Reset all UI state to empty. */
    private fun resetUI() {
        tabEditors.clear()
        sectionTexts.clear()
        tabPane.tabs.clear()
        assemblyViewer.clear()
        currentProject = null
        currentPromptName = null
        projectDir = null
        isDirty = false
        activeTemplate = null
        activeTemplateName = null
    }

    /** Create a new prompt in the current project. */
    private fun createNewPrompt() {
        val dialog = TextInputDialog().apply {
            title = "Создать новый промт"
            headerText = "Введите имя нового промта"
            contentText = "Имя промта:"
        }
        dialog.showAndWait().ifPresent { raw ->
            val name = raw.trim()
            if (name.isBlank()) return@ifPresent
            // Validate name: no forbidden chars
            if (!name.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                Alert(Alert.AlertType.ERROR, "Имя промта не должно содержать / \\ : * ? \" < > |").show()
                return@ifPresent
            }
            // Get all section keys (from sections + templates)
            val allKeys = getAllSectionKeys()
            val prompt = promptFileController.create(projectDir!!, name, allKeys.toList())
            currentPromptName = name
            isDirty = false
            applyPromptData(prompt.data)
            updateWindowTitle()
            updateMenuDisables()
        }
    }

    /** Open an existing prompt from the current project. */
    private fun openPrompt() {
        val prompts = promptFileController.list(projectDir!!)
        if (prompts.isEmpty()) {
            Alert(Alert.AlertType.INFORMATION, "Нет сохранённых промтов").show()
            return
        }
        val dialog = ChoiceDialog(prompts.first(), prompts).apply {
            title = "Открыть промт"
            headerText = "Выберите промт"
            contentText = "Промт:"
        }
        dialog.showAndWait().ifPresent { name ->
            val prompt = promptFileController.load(projectDir!!, name)
            if (prompt != null) {
                currentPromptName = name
                isDirty = false
                applyPromptData(prompt.data)
                updateWindowTitle()
                updateMenuDisables()
            }
        }
    }

    /** Save the current prompt. */
    private fun savePrompt() {
        if (currentPromptName == null) {
            savePromptAs()
            return
        }
        saveCurrentPrompt()
    }

    /** Save prompt as a new name. */
    private fun savePromptAs() {
        val dialog = TextInputDialog(currentPromptName ?: "").apply {
            title = "Сохранить как"
            headerText = "Введите имя нового промта"
            contentText = "Имя промта:"
        }
        dialog.showAndWait().ifPresent { raw ->
            val name = raw.trim()
            if (name.isBlank()) return@ifPresent
            if (!name.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                Alert(Alert.AlertType.ERROR, "Имя промта не должно содержать / \\ : * ? \" < > |").show()
                return@ifPresent
            }
            currentPromptName = name
            saveCurrentPrompt()
            updateWindowTitle()
            updateMenuDisables()
        }
    }

    /** Save current prompt data to file and update project.lastPrompt. */
    private fun saveCurrentPrompt() {
        if (currentPromptName == null || projectDir == null) return

        // Collect all section keys and their text
        val allKeys = getAllSectionKeys()
        val data = LinkedHashMap<String, String>()
        for (key in allKeys) {
            data[key] = sectionTexts[key] ?: ""
        }

        val prompt = Prompt(name = currentPromptName!!, data = data)
        promptFileController.save(projectDir!!, prompt)

        // Update project.lastPrompt
        val project = currentProject!!
        val updatedProject = project.copy(lastPrompt = currentPromptName)
        projectController.save(updatedProject, projectDir!!)
        currentProject = updatedProject

        isDirty = false
        updateWindowTitle()
    }

    /** Export assembled prompt to a .txt file. */
    private fun exportTxt() {
        assemblePrompt()
        saveToFile()
    }

    /** Import text from a .txt file into current sections. */
    private fun importTxt() {
        val chooser = FileChooser().apply {
            title = "Импорт из .txt"
            initialDirectory = java.nio.file.Paths.get(System.getProperty("user.home")).toFile()
        }
        chooser.showOpenDialog(primaryStage)?.let { file ->
            val content = Files.readString(file.toPath())
            tabEditors.forEach { (key, editor) -> sectionTexts[key] = editor.text }
            val parsed = parseTxtPrompt(content)
            for ((key, text) in parsed) sectionTexts[key] = text
            refreshSections()
            isDirty = true
            updateWindowTitle()
        }
    }

    /** Save assembled prompt to a file. */
    private fun saveToFile() {
        if (assemblyViewer.text.isBlank()) assemblePrompt()
        if (assemblyViewer.text.isBlank()) {
            Alert(Alert.AlertType.WARNING, "Нет текста для сохранения").show()
            return
        }
        val chooser = FileChooser().apply {
            title = "Сохранить промт"
            extensionFilters.add(FileChooser.ExtensionFilter("TXT файлы", "*.txt"))
        }
        chooser.showSaveDialog(primaryStage)?.let {
            Files.writeString(it.toPath(), assemblyViewer.text)
        }
    }

    /** Copy assembled prompt to clipboard. */
    private fun copyToClipboard() {
        val text = assemblyViewer.text
        if (text.isBlank()) {
            Alert(Alert.AlertType.WARNING, "Сначала соберите промт").show()
            return
        }
        val content = javafx.scene.input.ClipboardContent().apply { putString(text) }
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content)
    }

    // ─── Template menu actions ───────────────────────────────────────

    private fun openProjectTemplate() {
        val dir = projectDir ?: return
        val names = projectTemplateController.list(dir)
        if (names.isEmpty()) {
            Alert(Alert.AlertType.INFORMATION, "Нет шаблонов в проекте").show()
            return
        }
        val dialog = ChoiceDialog(activeTemplateName ?: names.first(), names).apply {
            title = "Открыть шаблон из проект��"
            headerText = "Выберите шаблон"
            contentText = "Шаблон:"
        }
        dialog.showAndWait().ifPresent { name ->
            applyTemplateFromProject(dir, name)
        }
    }

    private fun applyTemplateFromProject(dir: Path, name: String) {
        val loaded = projectTemplateController.load(dir, name) ?: return
        tabEditors.forEach { (key, editor) -> sectionTexts[key] = editor.text }
        activeTemplate = loaded
        activeTemplateName = loaded.name
        currentProject?.let { p ->
            val updated = p.copy(activeTemplate = loaded.name)
            projectController.save(updated, dir)
            currentProject = updated
        }
        refreshSections()
        isDirty = true
        updateWindowTitle()
    }

    private fun openSystemTemplate() {
        val names = systemTemplateController.list()
        if (names.isEmpty()) {
            Alert(Alert.AlertType.INFORMATION, "Системных шаблонов нет").show()
            return
        }
        val dialog = ChoiceDialog(names.first(), names).apply {
            title = "Открыть системный шаблон"
            headerText = "Выберите шаблон"
            contentText = "Шаблон:"
        }
        dialog.showAndWait().ifPresent { name ->
            val loaded = systemTemplateController.load(name) ?: return@ifPresent
            tabEditors.forEach { (key, editor) -> sectionTexts[key] = editor.text }
            activeTemplate = loaded
            activeTemplateName = loaded.name
            currentProject?.let { p ->
                projectDir?.let { dir ->
                    val updated = p.copy(activeTemplate = loaded.name)
                    projectController.save(updated, dir)
                    currentProject = updated
                }
            }
            refreshSections()
            isDirty = true
            updateWindowTitle()
        }
    }

    private fun saveActiveTemplate() {
        val dir = projectDir ?: return
        val t = activeTemplate ?: return
        projectTemplateController.save(dir, t)
        Alert(Alert.AlertType.INFORMATION, "Шаблон сохранён").show()
    }

    private fun saveActiveTemplateAs() {
        val dir = projectDir ?: return
        val t = activeTemplate ?: return
        val dialog = TextInputDialog(t.name).apply {
            title = "Сохранить шаблон как"
            headerText = "Имя нового шаблона"
            contentText = "Имя:"
        }
        dialog.showAndWait().ifPresent { raw ->
            val name = raw.trim()
            if (name.isBlank()) return@ifPresent
            if (!name.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                Alert(Alert.AlertType.ERROR, "Имя: только буквы, цифры, _ -").show()
                return@ifPresent
            }
            val copy = t.copy(name = name)
            projectTemplateController.save(dir, copy)
            activeTemplate = copy
            activeTemplateName = name
            currentProject?.let { p ->
                val updated = p.copy(activeTemplate = name)
                projectController.save(updated, dir)
                currentProject = updated
            }
        }
    }

    private fun saveActiveTemplateAsSystem() {
        val t = activeTemplate ?: return
        val dialog = TextInputDialog(t.name).apply {
            title = "Сохранить как системный"
            headerText = "Имя системного шаблона"
            contentText = "Имя:"
        }
        dialog.showAndWait().ifPresent { raw ->
            val name = raw.trim()
            if (name.isBlank()) return@ifPresent
            if (!name.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                Alert(Alert.AlertType.ERROR, "Имя: только буквы, цифры, _ -").show()
                return@ifPresent
            }
            val copy = t.copy(name = name)
            systemTemplateController.save(copy)
            Alert(Alert.AlertType.INFORMATION, "Системный шаблон сохранён").show()
        }
    }

    // ─── Section Editor ──────────────────────────────────────────────

    private fun openSectionEditor() {
        val t = activeTemplate ?: return
        val dialog = SectionEditorDialog(primaryStage, t.sections)
        val result = dialog.showAndWait()
        if (result.isPresent) {
            val newSections = result.get()
            tabEditors.forEach { (key, editor) -> sectionTexts[key] = editor.text }
            activeTemplate = t.copy(sections = newSections)
            refreshSections()
            sectionTexts.forEach { (key, text) -> tabEditors[key]?.text = text }
            isDirty = true
            updateWindowTitle()
        }
    }

    // ─── Spell Checking ──────────────────────────────────────────────

    private fun checkAllSpellcheck() {
        tabEditors.values.forEach { it.forceRecheck() }
        Alert(Alert.AlertType.INFORMATION, "Проверка всех разделов выполнена").show()
    }

    // ─── Assemble ────────────────────────────────────────────────────

    private fun assemblePrompt() {
        val displaySections = buildDisplaySections()
        val result = promptAssembler.assemblePrompt(displaySections, sectionTexts)
        assemblyViewer.text = result
    }

    // ─── UI Helpers ──────────────────────────────────────────────────

    /**
     * Build display sections from activeTemplate.sections.
     */
    private fun buildDisplaySections(): List<DisplaySection> {
        val configSections = activeTemplate?.sections ?: emptyList()
        val configKeys = configSections.map { it.key }.toSet()

        val allKeys = configSections.map { it.key }.toMutableSet()
        activeTemplate?.templates?.keys?.let { allKeys.addAll(it) }
        allKeys.addAll(sectionTexts.keys)

        val display = configSections.map { DisplaySection.fromSection(it) }.toMutableList()

        val implicitKeys = allKeys - configKeys
        var idx = 0
        for (key in implicitKeys) {
            display.add(DisplaySection(
                key = key,
                title = key,
                order = 1000 + idx,
                enabled = true,
                isImplicit = true
            ))
            idx++
        }

        return display
    }

    /** Get all section keys from active template + prompt data. */
    private fun getAllSectionKeys(): Set<String> {
        val configKeys = activeTemplate?.sections?.map { it.key }?.toSet() ?: emptySet()
        val allKeys = configKeys.toMutableSet()
        activeTemplate?.templates?.keys?.let { allKeys.addAll(it) }
        allKeys.addAll(sectionTexts.keys)
        return allKeys
    }

    /** Refresh tab editors based on active template sections. */
    private fun refreshSections() {
        tabEditors.forEach { (key, editor) -> sectionTexts[key] = editor.text }

        tabPane.tabs.clear()
        tabEditors.clear()

        val displaySections = buildDisplaySections()

        for (section in displaySections) {
            val editor = TabEditorView(
                section,
                spellChecker,
                sectionTexts[section.key] ?: section.defaultText
            )
            tabEditors[section.key] = editor
            if (sectionTexts[section.key] == null) {
                sectionTexts[section.key] = section.defaultText
            }

            // Apply templates from active template
            val formulations = activeTemplate?.templates?.get(section.key) ?: emptyList()
            editor.setTemplates(formulations)

            // Subscribe to text changes
            editor.textProperty.addListener { _, _, newVal ->
                sectionTexts[section.key] = newVal ?: ""
                if (!isApplyingData) {
                    isDirty = true
                    updateWindowTitle()
                }
            }
            editor.setOnTemplatesChanged {
                if (!isApplyingData) { isDirty = true; updateWindowTitle() }
            }

            val tab = Tab(section.title, editor).apply {
                isClosable = false
                tooltip = Tooltip(section.description.ifBlank { section.title })
            }
            tabPane.tabs.add(tab)
        }
    }

    /** Track if we're programmatically setting text to avoid isDirty. */
    private var isApplyingData = false

    /** Apply prompt data to all tabs. */
    private fun applyPromptData(data: Map<String, String>) {
        isApplyingData = true
        try {
            for ((key, text) in data) {
                sectionTexts[key] = text
                tabEditors[key]?.text = text
            }
        } finally {
            isApplyingData = false
        }
    }

    /** Update window title with dirty indicator. */
    private fun updateWindowTitle() {
        val project = currentProject
        val prompt = currentPromptName
        val base = if (project != null) project.name else "Prompt Editor v2.2"
        val suffix = prompt?.let { " — $it" } ?: ""
        val prefix = if (isDirty) "* " else ""
        primaryStage.title = "$prefix$base$suffix"
    }

    /** Parse TXT content (produced by assemblePrompt) back into sections. */
    private fun parseTxtPrompt(content: String): Map<String, String> {
        val displaySections = buildDisplaySections()
        val titleToKey = displaySections.associateBy(
            { if (it.isImplicit) it.key.uppercase() else it.title.uppercase() },
            { it.key }
        )
        val result = mutableMapOf<String, String>()
        var currentKey: String? = null
        val buffer = StringBuilder()

        fun flush() {
            currentKey?.let { result[it] = buffer.toString().trim() }
            buffer.clear()
        }

        for (line in content.lines()) {
            val trimmed = line.trim()
            val isSeparator = trimmed.length >= 20 && trimmed.all { it == '-' }
            val byTitle = titleToKey[trimmed]
            if (byTitle != null) {
                flush()
                currentKey = byTitle
            } else if (isSeparator) {
                // separator line after title — ignore
            } else {
                buffer.append(line).append('\n')
            }
        }
        flush()
        return result
    }

    /** Load default sections from config or classpath. */
    private fun loadDefaultSections(): List<Section> {
        val path = AppPaths.defaultSectionsPath
        if (Files.exists(path)) {
            val objectMapper = com.fasterxml.jackson.databind.ObjectMapper().apply {
                registerModule(com.fasterxml.jackson.module.kotlin.kotlinModule())
            }
            return objectMapper.readValue(
                path.toFile(),
                object : com.fasterxml.jackson.core.type.TypeReference<List<Section>>() {}
            )
        }
        // Fallback: try classpath
        val stream = javaClass.getResourceAsStream("/sections_config.json")
            ?: javaClass.getResourceAsStream("/config/sections_config.json")
            ?: return emptyList()
        stream.use { input ->
            val content = input.bufferedReader().readText()
            Files.writeString(path, content)
            val objectMapper = com.fasterxml.jackson.databind.ObjectMapper().apply {
                registerModule(com.fasterxml.jackson.module.kotlin.kotlinModule())
            }
            return objectMapper.readValue(content, object : com.fasterxml.jackson.core.type.TypeReference<List<Section>>() {})
        }
    }

    /** Try to auto-open the last project on startup. */
    private fun tryAutoOpenLastProject() {
        val lastPath = projectController.getLastProjectPath()
        if (lastPath != null && Files.exists(lastPath)) {
            try {
                openProjectAt(lastPath)
            } catch (_: Exception) {
                // Ignore — user will open manually
            }
        }
    }

    /** Open global settings dialog (workspace directory). */
    private fun openGlobalSettings() {
        val currentPath = AppPaths.resolveWorkspacePath()?.toString() ?: ""
        val dialog = Dialog<String>().apply {
            title = "Глобальные настройки"
            headerText = "Настройки приложения"
            dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        }
        val pathField = TextField(currentPath).apply {
            promptText = "Путь к рабочей папке проектов"
            prefColumnCount = 40
        }
        val browseBtn = Button("Обзор...")
        dialog.dialogPane.content = VBox(8.0, HBox(5.0, pathField, browseBtn)).apply {
            padding = Insets(10.0)
        }
        browseBtn.setOnAction {
            val chooser = DirectoryChooser().apply {
                title = "Выберите рабочую папку проектов"
                initialDirectory = Paths.get(System.getProperty("user.home")).toFile()
            }
            chooser.showDialog(primaryStage)?.let { pathField.text = it.path }
        }
        dialog.setResultConverter { bt ->
            if (bt == ButtonType.OK) pathField.text.trim() else null
        }
        val result = dialog.showAndWait().orElse(null) ?: return
        if (result.isEmpty()) {
            try { Files.deleteIfExists(AppPaths.workspacePath) } catch (_: Exception) {}
        } else {
            try {
                AppPaths.setWorkspacePath(Paths.get(result))
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Некорректный путь: ${e.message}").show()
                return
            }
        }
        Alert(Alert.AlertType.INFORMATION, "Настройки сохранены").show()
    }

    private fun showAbout() {
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "О программе"
            headerText = "Prompt Editor v2.2"
            contentText = "Редактор промтов.\nСтек: Kotlin + JavaFX 21"
        }.showAndWait()
    }
}

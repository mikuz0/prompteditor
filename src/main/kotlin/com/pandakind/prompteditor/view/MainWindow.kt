package com.pandakind.prompteditor.view

import com.pandakind.prompteditor.controller.ProfileController
import com.pandakind.prompteditor.controller.PromptController
import com.pandakind.prompteditor.controller.SpellCheckerService
import com.pandakind.prompteditor.model.Profile
import com.pandakind.prompteditor.model.Section
import com.pandakind.prompteditor.model.SectionManager
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

class MainWindow(
    private val primaryStage: Stage,
    private val sectionManager: SectionManager,
    private val profileController: ProfileController,
    private val promptController: PromptController,
    private val spellChecker: SpellCheckerService
) {

    private val profileLabel = Label("Профиль: (не загружен)")
    private val tabPane = TabPane()
    private val assemblyViewer = TextArea().apply { isEditable = false; isWrapText = true }
    private val tabEditors = mutableMapOf<String, TabEditorView>()
    private val sectionTexts = mutableMapOf<String, String>()

    init {
        primaryStage.title = "Prompt Editor v2.2"
        primaryStage.scene = buildScene()
        refreshSections()
        loadLastProfile()
    }

    fun show() {
        primaryStage.show()
    }

    private fun buildScene(): Scene {
        val topPanel = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(10.0)
            style = "-fx-background-color: #f0f0f0;"
            children.addAll(
                profileLabel.apply { style = "-fx-font-size: 14px; -fx-font-weight: bold;" },
                Button("Проверить орфографию").apply { setOnAction { checkAllSpellcheck() } },
                Button("Сбросить всё").apply { setOnAction { confirmReset() } }
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
        val fileMenu = Menu("Файл").apply {
            items.addAll(
                item("Сохранить") { saveCurrentProfile() },
                item("Сохранить как...") { saveProfileAs() },
                item("Загрузить...") { loadProfileDialog() },
                SeparatorMenuItem(),
                item("Экспорт JSON...") { exportJson() },
                item("Импорт JSON...") { importJson() },
                item("Экспорт TXT...") { exportTxt() },
                item("Импорт TXT...") { importTxt() },
                SeparatorMenuItem(),
                item("Выход") { primaryStage.close() }
            )
        }

        val settingsMenu = Menu("Настройки").apply {
            items.addAll(
                item("Редактор разделов...") { openSectionEditor() },
                SeparatorMenuItem(),
                item("Проверить все разделы") { checkAllSpellcheck() }
            )
        }

        val helpMenu = Menu("Справка").apply {
            items.add(item("О программе") { showAbout() })
        }

        return MenuBar(fileMenu, settingsMenu, helpMenu)
    }

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

    /** Перестроить вкладки по текущему SectionManager, сохранив уже введённые тексты. */
    private fun refreshSections() {
        // сохранить текущие тексты
        tabEditors.forEach { (key, editor) -> sectionTexts[key] = editor.text }

        tabPane.tabs.clear()
        tabEditors.clear()

        for (section in sectionManager.getEnabledSections()) {
            val editor = TabEditorView(section, spellChecker, sectionTexts[section.key] ?: section.defaultText)
            tabEditors[section.key] = editor
            if (sectionTexts[section.key] == null) {
                sectionTexts[section.key] = section.defaultText
            }
            editor.textProperty.addListener { _, _, newVal -> sectionTexts[section.key] = newVal ?: "" }

            val tab = Tab(section.title, editor).apply {
                isClosable = false
                tooltip = Tooltip(section.description.ifBlank { section.title })
            }
            tabPane.tabs.add(tab)
        }
    }

    private fun loadLastProfile() {
        val name = profileController.loadLastProfile()
        if (name == null) {
            profileLabel.text = "Профиль: (не загружен)"
            return
        }
        val profile = profileController.loadProfile(name)
        if (profile == null) {
            profileLabel.text = "Профиль: (не загружен)"
            return
        }
        applyProfile(profile)
        profileLabel.text = "Профиль: ${profile.name}"
    }

    private fun applyProfile(profile: Profile) {
        for ((key, text) in profile.profileData) {
            sectionTexts[key] = text
            tabEditors[key]?.text = text
        }
        for ((key, templates) in profile.templatesData) {
            tabEditors[key]?.setTemplates(templates)
        }
    }

    /** Собрать текущее состояние UI в Profile-объект. */
    private fun currentProfileSnapshot(name: String): Profile {
        val data = tabEditors.mapValues { it.value.text }
        val templates = tabEditors.mapValues { it.value.getTemplates() }
        return Profile(name, data, templates)
    }

    private fun assemblePrompt() {
        assemblyViewer.text = promptController.assemblePrompt(
            sectionManager.getEnabledSections(),
            tabEditors.mapValues { it.value.text }
        )
    }

    private fun copyToClipboard() {
        val text = assemblyViewer.text
        if (text.isBlank()) {
            Alert(Alert.AlertType.WARNING, "Сначала соберите промт").show()
            return
        }
        val content = javafx.scene.input.ClipboardContent().apply { putString(text) }
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content)
    }

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

    private fun saveCurrentProfile() {
        val name = profileController.loadLastProfile()
        if (name == null) {
            saveProfileAs()
            return
        }
        profileController.saveProfile(currentProfileSnapshot(name))
        profileLabel.text = "Профиль: $name"
    }

    private fun saveProfileAs() {
        val dialog = TextInputDialog().apply {
            title = "Сохранить как"
            headerText = "Введите имя профиля"
            contentText = "Имя профиля:"
        }
        dialog.showAndWait().ifPresent { raw ->
            val name = raw.trim()
            if (name.isBlank()) return@ifPresent
            profileController.saveProfile(currentProfileSnapshot(name))
            profileLabel.text = "Профиль: $name"
        }
    }

    private fun loadProfileDialog() {
        val profiles = profileController.listProfiles()
        if (profiles.isEmpty()) {
            Alert(Alert.AlertType.INFORMATION, "Нет сохранённых профилей").show()
            return
        }
        val dialog = ChoiceDialog(profiles.first(), profiles).apply {
            title = "Загрузить профиль"
            headerText = "Выберите профиль"
            contentText = "Профиль:"
        }
        dialog.showAndWait().ifPresent { name ->
            profileController.loadProfile(name)?.let {
                applyProfile(it)
                profileLabel.text = "Профиль: $name"
            }
        }
    }

    private fun exportJson() {
        val chooser = FileChooser().apply {
            extensionFilters.add(FileChooser.ExtensionFilter("JSON файлы", "*.json"))
        }
        val currentName = profileController.loadLastProfile() ?: "export"
        chooser.showSaveDialog(primaryStage)?.let { file ->
            val json = profileController.exportToJson(currentProfileSnapshot(currentName))
            Files.writeString(file.toPath(), json)
        }
    }

    private fun importJson() {
        val chooser = FileChooser().apply {
            extensionFilters.add(FileChooser.ExtensionFilter("JSON файлы", "*.json"))
        }
        chooser.showOpenDialog(primaryStage)?.let { file ->
            try {
                val profile = profileController.importFromJson(Files.readString(file.toPath()))
                applyProfile(profile)
                profileLabel.text = "Профиль: ${profile.name}"
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Не удалось импортировать JSON:\n${e.message}").show()
            }
        }
    }

    private fun exportTxt() {
        assemblePrompt()
        saveToFile()
    }

    private fun importTxt() {
        val chooser = FileChooser().apply {
            extensionFilters.add(FileChooser.ExtensionFilter("TXT файлы", "*.txt"))
        }
        chooser.showOpenDialog(primaryStage)?.let { file ->
            val parsed = parseTxtPrompt(
                Files.readString(file.toPath()),
                sectionManager.getEnabledSections()
            )
            for ((key, text) in parsed) {
                sectionTexts[key] = text
                tabEditors[key]?.text = text
            }
        }
    }

    /** Парсит сборку обратно в разделы по заголовкам. */
    private fun parseTxtPrompt(content: String, sections: List<Section>): Map<String, String> {
        val titleToKey = sections.associateBy({ it.title.uppercase() }, { it.key })
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
                // строка-разделитель под заголовком — игнор
            } else {
                buffer.append(line).append('\n')
            }
        }
        flush()
        return result
    }

    private fun openSectionEditor() {
        val dialog = SectionEditorDialog(primaryStage, sectionManager)
        dialog.showAndWait()
        if (dialog.hasChanges) {
            refreshSections()
        }
    }

    private fun checkAllSpellcheck() {
        tabEditors.values.forEach { it.forceRecheck() }
        Alert(Alert.AlertType.INFORMATION, "Проверка всех разделов выполнена").show()
    }

    private fun confirmReset() {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Сброс всех данных"
            headerText = "Внимание!"
            contentText = "Очистить все вкладки и панель сборки?"
        }
        alert.showAndWait().ifPresent { r ->
            if (r == ButtonType.OK) {
                tabEditors.values.forEach { it.text = "" }
                sectionTexts.clear()
                assemblyViewer.clear()
            }
        }
    }

    private fun showAbout() {
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "О программе"
            headerText = "Prompt Editor v2.2"
            contentText = "Редактор промтов.\nСтек: Kotlin + JavaFX 21"
        }.showAndWait()
    }
}
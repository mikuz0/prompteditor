package com.pandakind.prompteditor.view

import com.pandakind.prompteditor.model.Section
import com.pandakind.prompteditor.model.SectionManager
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage

class SectionEditorDialog(
    owner: Stage,
    private val sectionManager: SectionManager
) {
    private val stage = Stage()
    private val sectionList = ListView<String>()
    private val keyField = TextField()
    private val titleField = TextField()
    private val iconField = TextField()
    private val descriptionField = TextField()
    private val defaultTextField = TextArea()
    private val orderField = TextField()

    private var selectedKey: String? = null
    private var _hasChanges = false
    val hasChanges: Boolean get() = _hasChanges

    init {
        stage.initOwner(owner)
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Редактор разделов"
        buildUI()
        reloadList()
    }

    private fun buildUI() {
        val left = ScrollPane(sectionList).apply {
            minWidth = 220.0
            isFitToWidth = true
        }
        VBox.setVgrow(sectionList, Priority.ALWAYS)

        val form = GridPane().apply {
            hgap = 10.0
            vgap = 8.0
            padding = Insets(10.0)
        }
        var row = 0
        fun addRow(label: String, field: Control) {
            form.add(Label(label), 0, row)
            form.add(field, 1, row++)
        }
        keyField.promptText = "role, goal..."
        titleField.promptText = "Роль, Цель..."
        iconField.promptText = "иконка"
        descriptionField.promptText = "Описание..."
        defaultTextField.promptText = "Текст по умолчанию"
        defaultTextField.prefRowCount = 6
        orderField.promptText = "0, 1, 2..."

        addRow("Ключ:", keyField)
        addRow("Заголовок:", titleField)
        addRow("Иконка:", iconField)
        addRow("Описание:", descriptionField)
        addRow("Текст по умолчанию:", defaultTextField)
        addRow("Порядок:", orderField)

        val buttons = HBox(8.0).apply {
            padding = Insets(10.0)
            children.addAll(
                Button("Добавить").apply { setOnAction { startAdd() } },
                Button("Удалить").apply { setOnAction { deleteSelected() } },
                Button("↑ Вверх").apply { setOnAction { moveUp() } },
                Button("↓ Вниз").apply { setOnAction { moveDown() } },
                Button("Сохранить").apply { setOnAction { save() } },
                Button("Закрыть").apply { setOnAction { stage.close() } }
            )
        }

        val right = VBox(form, buttons).apply { padding = Insets(5.0) }
        val split = SplitPane(left, right).apply { setDividerPositions(0.3) }
        stage.scene = Scene(split, 760.0, 520.0)

        sectionList.selectionModel.selectedItemProperty().addListener { _, _, title ->
            if (title == null) {
                selectedKey = null
                clearForm()
            } else {
                sectionManager.getSectionByTitle(title)?.let { s ->
                    selectedKey = s.key
                    fillForm(s)
                }
            }
        }
    }

    private fun reloadList() {
        val titles = sectionManager.getAllSections().map { it.title }
        sectionList.items = FXCollections.observableArrayList(titles)
    }

    private fun fillForm(s: Section) {
        keyField.text = s.key
        titleField.text = s.title
        iconField.text = s.icon
        descriptionField.text = s.description
        defaultTextField.text = s.defaultText
        orderField.text = s.order.toString()
    }

    private fun clearForm() {
        keyField.clear()
        titleField.clear()
        iconField.clear()
        descriptionField.clear()
        defaultTextField.clear()
        orderField.clear()
    }

    private fun startAdd() {
        selectedKey = null
        clearForm()
        sectionList.selectionModel.clearSelection()
        keyField.requestFocus()
    }

    private fun save() {
        val key = keyField.text.trim()
        val title = titleField.text.trim()
        if (key.isEmpty() || title.isEmpty()) {
            alert(Alert.AlertType.ERROR, "Ключ и заголовок обязательны")
            return
        }
        if (!key.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
            alert(Alert.AlertType.ERROR, "Ключ: только буквы, цифры, _")
            return
        }
        val order = orderField.text.trim().toIntOrNull()
        if (order == null) {
            alert(Alert.AlertType.ERROR, "Порядок должен быть числом")
            return
        }
        val enabled = sectionManager.getSectionByKey(key)?.enabled ?: true
        val newSection = Section(
            key = key,
            title = title,
            icon = iconField.text.trim(),
            description = descriptionField.text.trim(),
            defaultText = defaultTextField.text,
            order = order,
            enabled = enabled
        )

        if (selectedKey == null) {
            if (sectionManager.getSectionByKey(key) != null) {
                alert(Alert.AlertType.ERROR, "Раздел с ключом '$key' уже существует")
                return
            }
            sectionManager.addSection(newSection)
        } else {
            val existing = sectionManager.getSectionByKey(selectedKey!!)
                ?: run { alert(Alert.AlertType.ERROR, "Раздел не найден"); return }
            if (existing.key != key && sectionManager.getSectionByKey(key) != null) {
                alert(Alert.AlertType.ERROR, "Раздел с ключом '$key' уже существует")
                return
            }
            // удалить старую запись, если ключ изменился, и вставить новую
            if (existing.key != key) {
                sectionManager.removeSection(existing.key)
                sectionManager.addSection(newSection)
            } else {
                sectionManager.updateSection(newSection)
            }
        }
        _hasChanges = true
        selectedKey = key
        reloadList()
        sectionList.selectionModel.select(title)
    }

    private fun deleteSelected() {
        val key = selectedKey ?: return
        val section = sectionManager.getSectionByKey(key) ?: return
        val ok = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Удаление"
            headerText = "Удалить раздел '${section.title}'?"
        }.showAndWait().orElse(ButtonType.CANCEL)
        if (ok == ButtonType.OK) {
            sectionManager.removeSection(key)
            _hasChanges = true
            selectedKey = null
            reloadList()
            clearForm()
        }
    }

    private fun moveUp() {
        val key = selectedKey ?: return
        try {
            sectionManager.moveSectionUp(key)
            _hasChanges = true
            reloadList()
            sectionManager.getSectionByKey(key)?.let { sectionList.selectionModel.select(it.title) }
        } catch (_: IllegalArgumentException) { /* уже сверху */ }
    }

    private fun moveDown() {
        val key = selectedKey ?: return
        try {
            sectionManager.moveSectionDown(key)
            _hasChanges = true
            reloadList()
            sectionManager.getSectionByKey(key)?.let { sectionList.selectionModel.select(it.title) }
        } catch (_: IllegalArgumentException) { /* уже снизу */ }
    }

    private fun alert(type: Alert.AlertType, message: String) {
        Alert(type, message).showAndWait()
    }

    fun showAndWait() = stage.showAndWait()
}
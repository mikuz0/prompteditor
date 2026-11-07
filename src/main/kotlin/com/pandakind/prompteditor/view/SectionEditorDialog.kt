package com.pandakind.prompteditor.view

import com.pandakind.prompteditor.model.Section
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage

class SectionEditorDialog(
    owner: Stage,
    initialSections: List<Section>
) : Dialog<List<Section>>() {

    private val sections: MutableList<Section> = initialSections.toMutableList()
    private val sectionList = ListView<String>()
    private val keyField = TextField()
    private val titleField = TextField()
    private val iconField = TextField()
    private val descriptionField = TextField()
    private val defaultTextField = TextArea()
    private val orderField = TextField()

    private var selectedKey: String? = null

    init {
        initOwner(owner)
        initModality(Modality.APPLICATION_MODAL)
        title = "Редактор разделов"
        buildUI()
        reloadList()
        dialogPane.buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        setResultConverter { bt ->
            if (bt == ButtonType.OK) sections.toList() else null
        }
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
                Button("Применить").apply { setOnAction { saveCurrent() } }
            )
        }

        val right = VBox(form, buttons).apply { padding = Insets(5.0) }
        val split = SplitPane(left, right).apply { setDividerPositions(0.3) }

        val content = VBox(split).apply { VBox.setVgrow(split, Priority.ALWAYS) }
        dialogPane.content = content
        dialogPane.prefWidth = 760.0
        dialogPane.prefHeight = 520.0

        sectionList.selectionModel.selectedItemProperty().addListener { _, _, title ->
            if (title == null) {
                selectedKey = null
                clearForm()
            } else {
                sections.find { it.title == title }?.let { s ->
                    selectedKey = s.key
                    fillForm(s)
                }
            }
        }
    }

    private fun reloadList() {
        val titles = sections.sortedBy { it.order }.map { it.title }
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

    private fun saveCurrent() {
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
        val enabled = sections.find { it.key == key }?.enabled ?: true
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
            if (sections.any { it.key == key }) {
                alert(Alert.AlertType.ERROR, "Раздел с ключом '$key' уже существует")
                return
            }
            sections.add(newSection)
        } else {
            val idx = sections.indexOfFirst { it.key == selectedKey }
            if (idx < 0) {
                alert(Alert.AlertType.ERROR, "Раздел не найден")
                return
            }
            if (existingKeyChangedAndConflicts(selectedKey!!, key)) {
                alert(Alert.AlertType.ERROR, "Раздел с ключом '$key' уже существует")
                return
            }
            sections[idx] = newSection
        }
        selectedKey = key
        reloadList()
        sectionList.selectionModel.select(title)
    }

    private fun existingKeyChangedAndConflicts(oldKey: String, newKey: String): Boolean {
        if (oldKey == newKey) return false
        return sections.any { it.key == newKey }
    }

    private fun deleteSelected() {
        val key = selectedKey ?: return
        val section = sections.find { it.key == key } ?: return
        val ok = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "Удаление"
            headerText = "Удалить раздел '${section.title}'?"
        }.showAndWait().orElse(ButtonType.CANCEL)
        if (ok == ButtonType.OK) {
            sections.removeAll { it.key == key }
            selectedKey = null
            reloadList()
            clearForm()
        }
    }

    private fun moveUp() {
        val key = selectedKey ?: return
        val sorted = sections.sortedBy { it.order }.toMutableList()
        val idx = sorted.indexOfFirst { it.key == key }
        if (idx <= 0) return
        val tmp = sorted[idx].order
        sorted[idx] = sorted[idx].copy(order = sorted[idx - 1].order)
        sorted[idx - 1] = sorted[idx - 1].copy(order = tmp)
        sections.clear()
        sections.addAll(sorted)
        reloadList()
        sections.find { it.key == key }?.let { sectionList.selectionModel.select(it.title) }
    }

    private fun moveDown() {
        val key = selectedKey ?: return
        val sorted = sections.sortedBy { it.order }.toMutableList()
        val idx = sorted.indexOfFirst { it.key == key }
        if (idx < 0 || idx >= sorted.size - 1) return
        val tmp = sorted[idx].order
        sorted[idx] = sorted[idx].copy(order = sorted[idx + 1].order)
        sorted[idx + 1] = sorted[idx + 1].copy(order = tmp)
        sections.clear()
        sections.addAll(sorted)
        reloadList()
        sections.find { it.key == key }?.let { sectionList.selectionModel.select(it.title) }
    }

    private fun alert(type: Alert.AlertType, message: String) {
        Alert(type, message).showAndWait()
    }
}

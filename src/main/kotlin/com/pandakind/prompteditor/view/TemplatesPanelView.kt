package com.pandakind.prompteditor.view

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.VBox

class TemplatesPanelView(
    @Suppress("unused") private val sectionKey: String,
    private val onInsertTemplate: (String) -> Unit
) : VBox(8.0) {

    private var onChanged: (() -> Unit)? = null
    fun setOnChanged(cb: () -> Unit) { onChanged = cb }

    private val templates: ObservableList<String> = FXCollections.observableArrayList()
    private val listView = ListView(templates).apply {
        minWidth = 180.0
        maxWidth = Double.MAX_VALUE
        tooltip = Tooltip("Двойной клик — вставить")
        setOnMouseClicked { e ->
            if (e.button == MouseButton.PRIMARY && e.clickCount == 2) {
                selectionModel.selectedItem?.let(onInsertTemplate)
            }
        }
    }

    init {
        val addBtn = Button("Добавить").apply {
            maxWidth = Double.MAX_VALUE
            setOnAction { showAddDialog()?.let { templates.add(it); onChanged?.invoke() } }
        }
        val removeBtn = Button("Удалить").apply {
            maxWidth = Double.MAX_VALUE
            setOnAction { listView.selectionModel.selectedItem?.let { templates.remove(it); onChanged?.invoke() } }
        }
        VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS)
        children.addAll(Label("Стандартные формулировки").apply { style = "-fx-font-weight: bold;" },
            listView, addBtn, removeBtn)
        padding = Insets(10.0)
        style = "-fx-background-color: #f9f9f9;"
    }

    fun getTemplates(): List<String> = templates.toList()

    fun setTemplates(list: List<String>) {
        templates.setAll(list)
    }

    private fun showAddDialog(): String? {
        val area = TextArea().apply {
            promptText = "Введите шаблонную формулировку..."
            prefRowCount = 6
            prefColumnCount = 40
        }
        val dialog = Dialog<String>().apply {
            title = "Добавить формулировку"
            headerText = "Новая шаблонная формулировка"
            dialogPane.content = VBox(8.0, area).apply { padding = Insets(10.0) }
            dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
            setResultConverter { bt ->
                if (bt == ButtonType.OK) area.text.trim().takeIf { it.isNotBlank() } else null
            }
        }
        return dialog.showAndWait().orElse(null)
    }
}
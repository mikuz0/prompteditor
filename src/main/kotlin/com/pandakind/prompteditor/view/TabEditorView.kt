package com.pandakind.prompteditor.view

import com.pandakind.prompteditor.controller.SpellCheckerService
import com.pandakind.prompteditor.model.DisplaySection
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.SplitPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class TabEditorView(
    private val section: DisplaySection,
    spellChecker: SpellCheckerService,
    initialText: String
) : VBox() {

    private val codeArea = SpellCheckedCodeArea(spellChecker, section.title)
    private val templatesPanel = TemplatesPanelView(section.key) { template ->
        codeArea.replaceText(codeArea.caretPosition, codeArea.caretPosition, template)
        codeArea.forceRecheck()
    }

    val textProperty: ObservableValue<String> get() = codeArea.textProperty

    var text: String
        get() = codeArea.text
        set(value) { codeArea.replaceText(0, codeArea.length, value) }

    init {
        codeArea.replaceText(0, codeArea.length, initialText)

        val split = SplitPane(codeArea, templatesPanel).apply {
            orientation = Orientation.HORIZONTAL
            setDividerPositions(0.7)
        }
        VBox.setVgrow(split, Priority.ALWAYS)

        children.add(split)
        padding = Insets(10.0)
    }

    fun getTemplates(): List<String> = templatesPanel.getTemplates()
    fun setTemplates(list: List<String>) = templatesPanel.setTemplates(list)
    fun forceRecheck() = codeArea.forceRecheck()
    fun setOnTemplatesChanged(cb: () -> Unit) = templatesPanel.setOnChanged(cb)
}

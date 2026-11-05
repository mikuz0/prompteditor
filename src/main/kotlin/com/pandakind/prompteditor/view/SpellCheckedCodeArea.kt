package com.pandakind.prompteditor.view

import com.pandakind.prompteditor.controller.SpellCheckerService
import javafx.animation.PauseTransition
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.layout.VBox
import javafx.util.Duration
import org.fxmisc.richtext.StyleClassedTextArea
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.Collections

class SpellCheckedCodeArea(
    private val spellChecker: SpellCheckerService,
    private val sectionTitle: String
) : VBox() {

    private val delegate = StyleClassedTextArea().apply {
        isWrapText = true
    }

    private val debounce = PauseTransition(Duration.millis(300.0)).also {
        it.setOnFinished { checkSpelling() }
    }
    private val contextMenu = ContextMenu()

    val textProperty: ObservableValue<String> = delegate.textProperty()

    init {
        children.add(delegate)
        padding = Insets(0.0)

        delegate.setOnContextMenuRequested { e ->
            buildContextMenu()
            contextMenu.show(delegate, e.screenX, e.screenY)
            e.consume()
        }
        delegate.textProperty().addListener { _, _, _ ->
            debounce.stop()
            debounce.playFromStart()
        }
    }

    // Delegate common methods
    val text: String get() = delegate.text
    val caretPosition: Int get() = delegate.caretPosition
    fun replaceText(start: Int, end: Int, text: String) = delegate.replaceText(start, end, text)
    fun setStyleClass(start: Int, end: Int, styleClass: String) = delegate.setStyleClass(start, end, styleClass)
    @Suppress("UNCHECKED_CAST")
    fun setStyleSpans(pos: Int, spans: StyleSpans<*>): Unit = delegate.setStyleSpans(pos, spans as StyleSpans<Collection<String>>)
    val length: Int get() = delegate.length

    fun checkSpelling() {
        val content = text
        if (content.isBlank()) {
            setStyleClass(0, length, "")
            return
        }
        val errors = spellChecker.checkText(content)
        applyHighlight(errors)
    }

    private fun buildContextMenu() {
        contextMenu.items.clear()
        val word = wordAtCaret()
        if (word == null || spellChecker.checkWord(word)) {
            contextMenu.items.add(MenuItem("Орфография в порядке").apply { isDisable = true })
            return
        }
        val suggestions = spellChecker.getSuggestions(word)
        if (suggestions.isEmpty()) {
            contextMenu.items.add(MenuItem("(нет предложений)").apply { isDisable = true })
        } else {
            suggestions.forEach { s ->
                contextMenu.items.add(MenuItem(s).apply {
                    setOnAction {
                        replaceWordAtCaret(s)
                        contextMenu.hide()
                    }
                })
            }
        }
        contextMenu.items.add(MenuItem("Добавить '$word' в словарь").apply {
            setOnAction {
                spellChecker.addWord(word)
                contextMenu.hide()
                checkSpelling()
            }
        })
        contextMenu.items.add(MenuItem("Пропустить").apply {
            setOnAction { contextMenu.hide() }
        })
    }

    private fun wordAtCaret(): String? {
        val text = text
        var pos = caretPosition
        if (text.isEmpty()) return null
        pos = pos.coerceIn(0, text.length - 1)
        if (!text[pos].isLetter()) return null
        var start = pos
        var end = pos + 1
        while (start > 0 && text[start - 1].isLetter()) start--
        while (end < text.length && text[end].isLetter()) end++
        return text.substring(start, end)
    }

    private fun replaceWordAtCaret(replacement: String) {
        val text = text
        var pos = caretPosition.coerceIn(0, text.length)
        if (text.isEmpty()) return
        if (pos == text.length || !text[pos.coerceAtMost(text.length - 1)].isLetter()) pos--
        if (pos < 0) return
        var start = pos
        var end = pos + 1
        while (start > 0 && text[start - 1].isLetter()) start--
        while (end < text.length && text[end].isLetter()) end++
        replaceText(start, end, replacement)
    }

    private fun applyHighlight(errors: Map<Int, List<String>>) {
        if (length == 0) return
        if (errors.isEmpty()) {
            setStyleClass(0, length, "")
            return
        }
        val spans: StyleSpans<Collection<String>> = buildSpans(text, errors.keys)
        setStyleSpans(0, spans)
    }

    private fun buildSpans(content: String, errorStarts: Set<Int>): StyleSpans<Collection<String>> {
        val builder = StyleSpansBuilder<Collection<String>>()
        var last = 0
        val starts = errorStarts.sorted()
        for (start in starts) {
            if (start < 0 || start >= content.length) continue
            var end = start
            while (end < content.length && content[end].isLetter()) end++
            if (end <= start) continue
            if (start > last) {
                builder.add(Collections.emptyList(), start - last)
            }
            builder.add(listOf("spell-error"), end - start)
            last = end
        }
        if (last < content.length) {
            builder.add(Collections.emptyList(), content.length - last)
        }
        return builder.create()
    }

    fun forceRecheck() = checkSpelling()

    fun setLanguage(language: String) {
        spellChecker.setLanguage(language)
        checkSpelling()
    }
}

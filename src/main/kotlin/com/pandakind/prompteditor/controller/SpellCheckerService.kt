package com.pandakind.prompteditor.controller

class SpellCheckerService {

    private var currentLanguage: String = "ru"

    fun checkWord(word: String, language: String = currentLanguage): Boolean = true

    fun getSuggestions(word: String, language: String = currentLanguage): List<String> = emptyList()

    fun addWord(word: String) { /* stub */ }

    fun checkText(text: String, language: String = currentLanguage): Map<Int, List<String>> = emptyMap()

    fun setLanguage(language: String) { currentLanguage = language }

    fun getLanguage(): String = currentLanguage
}

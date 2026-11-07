package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.util.AppPaths
import io.github.geniot.jortho.SpellCheckerOptions as JOrthoSpellCheckerOptions
import java.io.BufferedReader
import java.io.InputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Iterator
import java.util.Locale
import java.util.zip.InflaterInputStream

class SpellCheckerService {

    private var currentLanguage: String = "ru"
    private var dictionary: Any? = null
    private var options: JOrthoSpellCheckerOptions = JOrthoSpellCheckerOptions()
    private val userWords = mutableSetOf<String>()

    // Reflection caches
    private val dictionaryFactoryClass: Class<*> = Class.forName("io.github.geniot.jortho.DictionaryFactory")
    private val dictionaryClass: Class<*> = Class.forName("io.github.geniot.jortho.Dictionary")
    private val languageBundleClass: Class<*> = Class.forName("io.github.geniot.jortho.LanguageBundle")
    private val suggestionClass: Class<*> = Class.forName("io.github.geniot.jortho.Suggestion")

    private val factoryConstructor: Constructor<*> = dictionaryFactoryClass.getDeclaredConstructor()
    private val factoryLoadWordsMethod: Method = dictionaryFactoryClass.getDeclaredMethod("loadWords", Iterator::class.java)
    private val factoryCreateMethod: Method = dictionaryFactoryClass.getDeclaredMethod("create")
    private val dictionaryExistMethod: Method = dictionaryClass.getSuperclass().getDeclaredMethod("exist", String::class.java)
    private val dictionarySearchMethod: Method = dictionaryClass.getMethod("searchSuggestions", String::class.java)
    private val languageBundleGetMethod: Method = languageBundleClass.getDeclaredMethod("get", Locale::class.java)
    private val languageBundleExistMethod: Method = languageBundleClass.getDeclaredMethod(
        "existInDictionary", String::class.java, dictionaryClass, JOrthoSpellCheckerOptions::class.java, Boolean::class.java
    )
    private val suggestionToStringMethod: Method = suggestionClass.getDeclaredMethod("toString")

    init {
        factoryConstructor.isAccessible = true
        factoryLoadWordsMethod.isAccessible = true
        factoryCreateMethod.isAccessible = true
        dictionaryExistMethod.isAccessible = true
        dictionarySearchMethod.isAccessible = true
        languageBundleGetMethod.isAccessible = true
        languageBundleExistMethod.isAccessible = true
        suggestionToStringMethod.isAccessible = true
        loadUserWords()
        loadDictionary(currentLanguage)
    }

    private fun loadUserWords() {
        val path = AppPaths.userDictPath
        if (!java.nio.file.Files.exists(path)) return
        try {
            java.nio.file.Files.readAllLines(path)
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .forEach { userWords.add(it) }
        } catch (_: Exception) {}
    }

    private fun saveUserWords() {
        try {
            java.nio.file.Files.createDirectories(AppPaths.userDictPath.parent)
            java.nio.file.Files.write(
                AppPaths.userDictPath,
                userWords.sorted()
            )
        } catch (_: Exception) {}
    }

    private fun loadDictionary(lang: String) {
        val resourceName = "dictionary_$lang.ortho"
        val stream: InputStream? = javaClass.getResourceAsStream("/$resourceName")
            ?: javaClass.getResourceAsStream(resourceName)
        if (stream == null) {
            dictionary = null
            return
        }
        try {
            // .ortho files are zlib-compressed plain text word lists
            val decompressor = InflaterInputStream(stream)
            val reader = BufferedReader(java.io.InputStreamReader(decompressor, Charsets.UTF_8))
            val words = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val word = line!!.removePrefix(" ").trim()
                if (word.isNotEmpty()) words.add(word)
            }
            reader.close()

            factoryConstructor.isAccessible = true
            val factory = factoryConstructor.newInstance()
            factoryLoadWordsMethod.invoke(factory, words.iterator())
            dictionary = factoryCreateMethod.invoke(factory)
        } catch (_: Throwable) {
            dictionary = null
        }
    }

    private fun getLanguageBundle(): Any? {
        return try {
            val locale = when (currentLanguage) {
                "en" -> Locale.ENGLISH
                "ru" -> Locale.forLanguageTag("ru")
                else -> Locale.getDefault()
            }
            languageBundleGetMethod.invoke(null, locale)
        } catch (_: Throwable) {
            null
        }
    }

    fun checkWord(word: String, language: String = currentLanguage): Boolean {
        val clean = word.trim().lowercase()
        if (clean.isEmpty() || clean.length < 2) return true
        if (clean in userWords) return true
        val dict = dictionary ?: return true
        val bundle = getLanguageBundle() ?: return true
        return try {
            @Suppress("UNCHECKED_CAST")
            languageBundleExistMethod.invoke(bundle, clean, dict, options, true) as Boolean
        } catch (_: Throwable) {
            true
        }
    }

    fun getSuggestions(word: String, language: String = currentLanguage): List<String> {
        val clean = word.trim().lowercase()
        if (clean.isBlank()) return emptyList()
        val dict = dictionary ?: return emptyList()
        return try {
            @Suppress("UNCHECKED_CAST")
            val suggestions = dictionarySearchMethod.invoke(dict, clean) as List<*>
            suggestions.mapNotNull { suggestionToStringMethod.invoke(it) as? String }.take(8)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun addWord(word: String) {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return
        if (userWords.add(clean)) {
            saveUserWords()
        }
    }

    fun checkText(text: String, language: String = currentLanguage): Map<Int, List<String>> {
        if (text.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, List<String>>()
        val wordRegex = Regex("[\\p{L}][\\p{L}'-]*")
        for (m in wordRegex.findAll(text)) {
            val word = m.value
            if (word.length < 2) continue
            if (!checkWord(word, language)) {
                result[m.range.first] = getSuggestions(word, language)
            }
        }
        return result
    }

    fun setLanguage(language: String) {
        currentLanguage = language
        loadDictionary(language)
    }

    fun getLanguage(): String = currentLanguage
}

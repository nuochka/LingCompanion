package com.app.lingcompanion.ui

import android.content.Context
import com.app.lingcompanion.ui.myWords.Word
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*

object WordFileManager {

    private const val FILENAME = "words.json"

    fun saveWord(context: Context, word: Word) {
        val words = loadWords(context).toMutableList()
        words.add(word)
        saveWords(context, words)
    }

    private fun saveWords(context: Context, words: List<Word>) {
        try {
            context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    val gson = Gson()
                    val wordsJson = gson.toJson(words)
                    writer.write(wordsJson)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun wordExists(context: Context, word: Word): Boolean {
        val words = loadWords(context)
        return words.any { it == word }
    }

    fun loadWords(context: Context): List<Word> {
        try {
            context.openFileInput(FILENAME).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val gson = Gson()
                    val type = object : TypeToken<List<Word>>() {}.type
                    return gson.fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (e: FileNotFoundException) {
            // File is not found
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return emptyList()
    }

    fun deleteWord(context: Context, word: Word) {
        val words = loadWords(context).toMutableList()
        words.remove(word)
        saveWords(context, words)
    }
}

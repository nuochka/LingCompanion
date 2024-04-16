package com.app.lingcompanion.ui

import android.content.Context
import com.app.lingcompanion.ui.myWords.Word
import com.google.gson.Gson
import java.io.*

object WordFileManager {

    private const val FILENAME = "words.json"

    fun saveWord(context: Context, word: Word) {
        try {
            val outputStream = context.openFileOutput(FILENAME, Context.MODE_APPEND)
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            val gson = Gson()
            val wordJson = gson.toJson(word)
            writer.write("$wordJson\n")
            writer.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadWords(context: Context): List<Word> {
        val words = mutableListOf<Word>()
        try {
            val inputStream = context.openFileInput(FILENAME)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val gson = Gson()
                val word = gson.fromJson(line, Word::class.java)
                words.add(word)
            }
            reader.close()
            inputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return words
    }

    fun deleteWord(context: Context, word: Word) {
        val tempFile = File(context.filesDir, "temp.json")
        try {
            val inputStream = context.openFileInput(FILENAME)
            val outputStream = FileOutputStream(tempFile)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val gson = Gson()
                val currentWord = gson.fromJson(line, Word::class.java)
                if (currentWord != word) {
                    val wordJson = gson.toJson(currentWord)
                    writer.write("$wordJson\n")
                }
            }
            reader.close()
            writer.close()
            inputStream.close()
            outputStream.close()
            tempFile.renameTo(File(context.filesDir, FILENAME))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

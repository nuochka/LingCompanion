package com.app.lingcompanion.ui
import android.content.Context
import java.io.*

object WordFileManager {

    private const val FILENAME = "words.txt"

    fun saveWord(context: Context, word: String) {
        try {
            val outputStream = context.openFileOutput(FILENAME, Context.MODE_APPEND)
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            writer.write("$word\n")
            writer.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadWords(context: Context): Set<String> {
        val words = HashSet<String>()
        try {
            val inputStream = context.openFileInput(FILENAME)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { words.add(it) }
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


    fun deleteWord(context: Context, word: String) {
        val tempFile = File(context.filesDir, "temp.txt")
        try {
            val inputStream = context.openFileInput(FILENAME)
            val outputStream = FileOutputStream(tempFile)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line != word) {
                    writer.write("$line\n")
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

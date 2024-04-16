package com.app.lingcompanion.ui.myWords

data class Word(
    val word: String,
    val phonetic: String?,
    val partOfSpeech: String?,
    val definition: String?,
    val example: String?
)

package com.app.lingcompanion.ui.chat
import java.io.FileInputStream
import java.util.*

object ApiConfig {
    private val properties = Properties()

    init {
        val configFilePath = "config.properties"
        val inputStream = FileInputStream(configFilePath)
        properties.load(inputStream)
    }

    fun getApiKey(): String {
        return properties.getProperty("api.key")
    }
}
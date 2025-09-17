package com.yourcompany.yourapp

import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

object IniHelper {
    private val iniFile = File(Environment.getExternalStorageDirectory(), "MyScripts/scripts.ini")
    private val properties = Properties()

    init {
        iniFile.parentFile?.mkdirs()
        if (iniFile.exists()) {
            FileInputStream(iniFile).use { properties.load(it) }
        }
    }

    fun getScriptConfig(scriptName: String): ScriptConfig {
        return ScriptConfig(
            name = properties.getProperty("[$scriptName]name", ""),
            description = properties.getProperty("[$scriptName]description", ""),
            icon = properties.getProperty("[$scriptName]icon", ""),
            isActive = properties.getProperty("[$scriptName]is_active", "true").toBoolean(),
            hasShortcut = properties.getProperty("[$scriptName]has_shortcut", "false").toBoolean()
        )
    }

    fun updateScriptConfig(scriptName: String, config: ScriptConfig) {
        properties.setProperty("[$scriptName]name", config.name)
        properties.setProperty("[$scriptName]description", config.description)
        properties.setProperty("[$scriptName]icon", config.icon)
        properties.setProperty("[$scriptName]is_active", config.isActive.toString())
        properties.setProperty("[$scriptName]has_shortcut", config.hasShortcut.toString())
        save()
    }

    fun addScriptConfig(scriptName: String, config: ScriptConfig) {
        updateScriptConfig(scriptName, config)
    }

    fun renameScriptConfig(oldName: String, newName: String, config: ScriptConfig) {
        properties.remove("[$oldName]name")
        properties.remove("[$oldName]description")
        properties.remove("[$oldName]icon")
        properties.remove("[$oldName]is_active")
        properties.remove("[$oldName]has_shortcut")
        updateScriptConfig(newName, config)
    }

    fun deleteScriptConfig(scriptName: String) {
        properties.remove("[$scriptName]name")
        properties.remove("[$scriptName]description")
        properties.remove("[$scriptName]icon")
        properties.remove("[$scriptName]is_active")
        properties.remove("[$scriptName]has_shortcut")
        save()
    }

    private fun save() {
        FileOutputStream(iniFile).use { properties.store(it, null) }
    }
}
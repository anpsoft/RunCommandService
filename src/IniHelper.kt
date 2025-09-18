package com.yourcompany.yourapp

import android.os.Environment
import org.ini4j.Ini
import java.io.File

object IniHelper {
    private val iniFile = File(Environment.getExternalStorageDirectory(), "MyScripts/scripts.ini")
    private val ini = Ini()

    init {
        iniFile.parentFile?.mkdirs()
        if (iniFile.exists()) {
            ini.load(iniFile)
        }
    }

    fun getScriptConfig(scriptName: String): ScriptConfig {
        val section = ini[scriptName] ?: return ScriptConfig()
        return ScriptConfig(
            name = section.get("name", ""),
            description = section.get("description", ""),
            icon = section.get("icon", ""),
            isActive = section.get("is_active", "true").toBoolean(),
            hasShortcut = section.get("has_shortcut", "false").toBoolean()
        )
    }

    fun updateScriptConfig(scriptName: String, config: ScriptConfig) {
        val section = ini.add(scriptName)
        section["name"] = config.name
        section["description"] = config.description
        section["icon"] = config.icon
        section["is_active"] = config.isActive.toString()
        section["has_shortcut"] = config.hasShortcut.toString()
        save()
    }

    fun addScriptConfig(scriptName: String, config: ScriptConfig) {
        updateScriptConfig(scriptName, config)
    }

    fun renameScriptConfig(oldName: String, newName: String, config: ScriptConfig) {
        ini.remove(oldName)
        updateScriptConfig(newName, config)
    }

    fun deleteScriptConfig(scriptName: String) {
        ini.remove(scriptName)
        save()
    }

    private fun save() {
        ini.store(iniFile)
    }
}
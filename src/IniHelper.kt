package com.yourcompany.yourapp6

import android.content.Context
import android.content.SharedPreferences
import org.ini4j.Ini
import java.io.File

object IniHelper {
    private lateinit var iniFile: File
    private val ini = Ini()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val defaultScriptsDir = "/sdcard/MyScripts"
        val defaultIconsDir = "/sdcard/MyScripts/icons"

        var scriptsDir = prefs.getString("scripts_dir", defaultScriptsDir) ?: defaultScriptsDir
        var iconsDir = prefs.getString("icons_dir", defaultIconsDir) ?: defaultIconsDir

        iniFile = File(scriptsDir, "scripts.ini")
        iniFile.parentFile?.mkdirs()

        if (iniFile.exists()) {
            ini.load(iniFile)
        }
        // Чтение секции settings без создания!
        val section = ini["settings"]
        val iniScripts = section?.get("scripts_dir")
        if (!iniScripts.isNullOrEmpty() && iniScripts != defaultScriptsDir) {
            scriptsDir = iniScripts
            prefs.edit().putString("scripts_dir", scriptsDir).apply()
        }
        val iniIcons = section?.get("icons_dir")
        if (!iniIcons.isNullOrEmpty() && iniIcons != defaultIconsDir) {
            iconsDir = iniIcons
            prefs.edit().putString("icons_dir", iconsDir).apply()
        }
        iniFile = File(scriptsDir, "scripts.ini")
    }

    private fun getSettingsFile(context: Context): File {
        return File(getScriptsDir(context), "scripts.ini")
    }

    fun getScriptsDir(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val section = ini["settings"]
        val iniValue = section?.get("scripts_dir")
        if (!iniValue.isNullOrEmpty()) {
            return iniValue
        }
        return prefs.getString("scripts_dir", "/sdcard/MyScripts") ?: "/sdcard/MyScripts"
    }

    fun getIconsDir(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val section = ini["settings"]
        val iniValue = section?.get("icons_dir")
        if (!iniValue.isNullOrEmpty()) {
            return iniValue
        }
        return prefs.getString("icons_dir", "/sdcard/MyScripts/icons") ?: "/sdcard/MyScripts/icons"
    }

    fun updateSettings(context: Context, scriptsDir: String, iconsDir: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("scripts_dir", scriptsDir).putString("icons_dir", iconsDir).apply()
        var section = ini["settings"]
        if (section == null) section = ini.add("settings")
        section.put("scripts_dir", scriptsDir)
        section.put("icons_dir", iconsDir)
        save()
        iniFile = File(scriptsDir, "scripts.ini")
    }

    fun getScriptConfig(scriptName: String): ScriptConfig {
        return try {
            val section = ini[scriptName]
            if (section != null) {
                ScriptConfig(
                    name = section.get("name", ""),
                    description = section.get("description", ""),
                    icon = section.get("icon", ""),
                    isActive = section.get("is_active", "true").toBoolean(),
                    hasShortcut = section.get("has_shortcut", "false").toBoolean()
                )
            } else {
                ScriptConfig(isActive = true)
            }
        } catch (e: Exception) {
            ScriptConfig(isActive = true)
        }
    }

    fun updateScriptConfig(scriptName: String, config: ScriptConfig) {
        var section = ini[scriptName]
        if (section == null) section = ini.add(scriptName)
        section.put("name", config.name)
        section.put("description", config.description)
        section.put("icon", config.icon)
        section.put("is_active", config.isActive.toString())
        section.put("has_shortcut", config.hasShortcut.toString())
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

    fun cleanupOrphanedConfigs(context: Context) {
        val scriptsDir = File(getScriptsDir(context))
        val existingFiles = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }
            ?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()

        val sectionsToRemove = mutableListOf<String>()
        for (sectionName in ini.keys) {
            if (!existingFiles.contains(sectionName) && sectionName != "settings") {
                sectionsToRemove.add(sectionName)
            }
        }

        sectionsToRemove.forEach { ini.remove(it) }
        if (sectionsToRemove.isNotEmpty()) {
            save()
        }
    }

    fun createShortcutsForExisting(context: Context) {
        for (sectionName in ini.keys) {
            if (sectionName == "settings") continue
            val config = getScriptConfig(sectionName)
            if (config.hasShortcut) {
                val scriptPath = "${getScriptsDir(context)}/$sectionName.sh"
                ShortcutManager.createShortcut(context, sectionName, scriptPath, config.icon)
            }
        }
    }

    private fun save() {
        try {
            ini.store(iniFile)
        } catch (e: Exception) {
            android.util.Log.e("IniHelper", "Failed to save ini file: ${e.message}")
        }
    }
}
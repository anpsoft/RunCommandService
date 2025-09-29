package com.yourcompany.yourapp6

import android.content.Context
import java.io.File
import android.app.AlertDialog
import android.util.Log

object IniHelper {
    private lateinit var iniFile: File
    private val sections = mutableMapOf<String, MutableMap<String, String>>()
    private const val MAX_INI_SIZE = 1024 // 1 KB
    private var appContext: Context? = null

    // Закомментировано - не работает
    /*private fun checkIniSize() {
        if (iniFile.exists() && iniFile.length() > MAX_INI_SIZE) {
            appContext?.let { ctx ->
                AlertDialog.Builder(ctx)
                    .setTitle("Предупреждение")
                    .setMessage("INI-файл превысил 1 КБ. Возможны проблемы с его целостностью.")
                    .setPositiveButton("ОК", null)
                    .show()
            } ?: Log.e("IniHelper", "INI file size exceeds 1 KB, context not initialized.")
        }
    }*/

    private fun loadIni() {
        sections.clear()
        if (!iniFile.exists()) return

        try {
            var currentSection = ""
            iniFile.forEachLine { line ->
                val trimmed = line.trim()
                when {
                    trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";") -> {
                        // Пропускаем пустые строки и комментарии
                    }
                    trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                        currentSection = trimmed.substring(1, trimmed.length - 1)
                        sections.putIfAbsent(currentSection, mutableMapOf())
                    }
                    trimmed.contains("=") && currentSection.isNotEmpty() -> {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            sections[currentSection]?.put(parts[0].trim(), parts[1].trim())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IniHelper", "Failed to load ini file: ${e.message}")
        }
    }

    private fun saveIni() {
        synchronized(sections) {
            try {
                iniFile.parentFile?.mkdirs()
                iniFile.writeText(buildString {
                    sections.forEach { (sectionName, keys) ->
                        append("[$sectionName]\n")
                        keys.forEach { (key, value) ->
                            append("$key=$value\n")
                        }
                        append("\n")
                    }
                })
                // checkIniSize() // Закомментировано
            } catch (e: Exception) {
                Log.e("IniHelper", "Failed to save ini file: ${e.message}")
            }
        }
    }

    private fun readIni(sectionName: String, key: String, defaultValue: String): String {
        return sections[sectionName]?.get(key) ?: defaultValue
    }

    private fun writeIni(sectionName: String, key: String, value: String) {
        synchronized(sections) {
            sections.putIfAbsent(sectionName, mutableMapOf())
            sections[sectionName]?.put(key, value)
            saveIni()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext!!.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val defaultScriptsDir = "/sdcard/MyScripts"
        val defaultIconsDir = "/sdcard/MyScripts/icons"

        var scriptsDir = prefs.getString("scripts_dir", defaultScriptsDir) ?: defaultScriptsDir
        var iconsDir = prefs.getString("icons_dir", defaultIconsDir) ?: defaultIconsDir

        iniFile = File(scriptsDir, "scripts.ini")
        iniFile.parentFile?.mkdirs()

        loadIni()

        val iniScripts = readIni("settings", "scripts_dir", defaultScriptsDir)
        if (iniScripts != defaultScriptsDir) {
            scriptsDir = iniScripts
            prefs.edit().putString("scripts_dir", scriptsDir).apply()
        }
        val iniIcons = readIni("settings", "icons_dir", defaultIconsDir)
        if (iniIcons != defaultIconsDir) {
            iconsDir = iniIcons
            prefs.edit().putString("icons_dir", iconsDir).apply()
        }
        iniFile = File(scriptsDir, "scripts.ini")
        loadIni()
    }

    fun getScriptsDir(): String {
        return readIni("settings", "scripts_dir", "/sdcard/MyScripts")
    }

    fun getIconsDir(): String {
        return readIni("settings", "icons_dir", "/sdcard/MyScripts/icons")
    }

    fun updateSettings(scriptsDir: String, iconsDir: String) {
        val prefs = appContext?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) ?: return
        prefs.edit().putString("scripts_dir", scriptsDir).putString("icons_dir", iconsDir).apply()
        writeIni("settings", "scripts_dir", scriptsDir)
        writeIni("settings", "icons_dir", iconsDir)
        iniFile = File(scriptsDir, "scripts.ini")
        loadIni()
    }

    fun getScriptConfig(scriptName: String): ScriptConfig {
        return if (sections.containsKey(scriptName)) {
            ScriptConfig(
                name = readIni(scriptName, "name", ""),
                description = readIni(scriptName, "description", ""),
                icon = readIni(scriptName, "icon", ""),
                isActive = readIni(scriptName, "is_active", "true").toBoolean(),
                hasShortcut = readIni(scriptName, "has_shortcut", "false").toBoolean()
            )
        } else {
            ScriptConfig(isActive = true)
        }
    }

    fun updateScriptConfig(scriptName: String, config: ScriptConfig) {
        writeIni(scriptName, "name", config.name)
        writeIni(scriptName, "description", config.description)
        writeIni(scriptName, "icon", config.icon)
        writeIni(scriptName, "is_active", config.isActive.toString())
        writeIni(scriptName, "has_shortcut", config.hasShortcut.toString())
    }

    fun addScriptConfig(scriptName: String, config: ScriptConfig) {
        updateScriptConfig(scriptName, config)
    }

    fun renameScriptConfig(oldName: String, newName: String, config: ScriptConfig) {
        synchronized(sections) {
            sections.remove(oldName)
            updateScriptConfig(newName, config)
        }
    }

    fun deleteScriptConfig(scriptName: String) {
        synchronized(sections) {
            sections.remove(scriptName)
            saveIni()
        }
    }

    fun cleanupOrphanedConfigs() {
        val scriptsDir = File(getScriptsDir())
        val existingFiles = scriptsDir.listFiles { _, name -> name.endsWith(".sh") }
            ?.map { it.nameWithoutExtension }?.toSet() ?: emptySet()

        synchronized(sections) {
            val sectionsToRemove = sections.keys.filter { it != "settings" && !existingFiles.contains(it) }
            sectionsToRemove.forEach { sections.remove(it) }
            if (sectionsToRemove.isNotEmpty()) {
                saveIni()
            }
        }
    }

    fun createShortcutsForExisting(context: Context) {
        for (sectionName in sections.keys) {
            if (sectionName == "settings") continue
            val config = getScriptConfig(sectionName)
            if (config.hasShortcut) {
                val scriptPath = "${getScriptsDir()}/$sectionName.sh"
                ShortcutManager.createShortcut(context, sectionName, scriptPath, config.icon)
            }
        }
    }
}
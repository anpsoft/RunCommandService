package com.yourcompany.yourapp6

import android.content.Context
import org.ini4j.Ini
import java.io.File
import android.app.AlertDialog
import android.util.Log

object IniHelper {
    private lateinit var iniFile: File
    private val ini = Ini()
    private const val MAX_INI_SIZE = 1024 // 1 KB
    private var appContext: Context? = null

    private fun checkIniSize() {
        if (iniFile.exists() && iniFile.length() > MAX_INI_SIZE) {
            appContext?.let { ctx ->
                AlertDialog.Builder(ctx)
                    .setTitle("Предупреждение")
                    .setMessage("INI-файл превысил 1 КБ. Возможны проблемы с его целостностью.")
                    .setPositiveButton("ОК", null)
                    .show()
            } ?: Log.e("IniHelper", "INI file size exceeds 1 KB, context not initialized.")
        }
    }

    private fun readIni(sectionName: String, key: String, defaultValue: String): String {
        val section = ini[sectionName]
        return section?.get(key) ?: defaultValue
    }

    private fun writeIni(sectionName: String, key: String, value: String) {
        synchronized(ini) {
            val section = ini[sectionName] ?: ini.add(sectionName)
            if (section[key] != value) {
                section.put(key, value)
                save()
            }
        }
    }

    private fun save() {
        synchronized(ini) {
            try {
                ini.store(iniFile)
                checkIniSize()
            } catch (e: Exception) {
                Log.e("IniHelper", "Failed to save ini file: ${e.message}")
            }
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

        if (iniFile.exists()) {
            try {
                ini.load(iniFile)
            } catch (e: Exception) {
                Log.e("IniHelper", "Failed to load ini file: ${e.message}")
            }
        }

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
    }

    fun getScriptsDir(): String {
        return readIni("settings", "scripts_dir", "/sdcard/MyScripts")
    }

    fun getIconsDir(): String {
        return readIni("settings", "icons_dir", "/sdcard/MyScripts/icons")
    }

    fun updateSettings(/* context: Context,  */scriptsDir: String, iconsDir: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("scripts_dir", scriptsDir).putString("icons_dir", iconsDir).apply()
        writeIni("settings", "scripts_dir", scriptsDir)
        writeIni("settings", "icons_dir", iconsDir)
        iniFile = File(scriptsDir, "scripts.ini")
    }

    fun getScriptConfig(scriptName: String): ScriptConfig {
        val section = ini[scriptName]
        return if (section != null) {
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
        ini.remove(oldName)
        updateScriptConfig(newName, config)
    }

    fun deleteScriptConfig(scriptName: String) {
        ini.remove(scriptName)
        save()
    }

    fun cleanupOrphanedConfigs() {
        val scriptsDir = File(getScriptsDir())
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
                val scriptPath = "${getScriptsDir()}/$sectionName.sh"
                ShortcutManager.createShortcut(context, sectionName, scriptPath, config.icon)
            }
        }
    }
}
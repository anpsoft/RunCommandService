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

    private fun checkIniSize(context: Context) {
        if (iniFile.exists() && iniFile.length() > MAX_INI_SIZE) {
            AlertDialog.Builder(context)
                .setTitle("Предупреждение")
                .setMessage("INI-файл превысил 1 КБ. Возможны проблемы с его целостностью.")
                .setPositiveButton("ОК", null)
                .show()
        }
    }

    private fun readIni(context: Context, sectionName: String, key: String, defaultValue: String): String {
        checkIniSize(context)
        val section = ini[sectionName]
        return section?.get(key) ?: defaultValue
    }

    private fun writeIni(context: Context, sectionName: String, key: String, value: String) {
        synchronized(ini) {
            val section = ini[sectionName] ?: ini.add(sectionName)
            if (section[key] != value) {
                section.put(key, value)
                save(context)
            }
        }
    }

    private fun save(context: Context) {
        synchronized(ini) {
            try {
                ini.store(iniFile)
                checkIniSize(context)
            } catch (e: Exception) {
                Log.e("IniHelper", "Failed to save ini file: ${e.message}")
            }
        }
    }

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
        val iniScripts = readIni(context, "settings", "scripts_dir", defaultScriptsDir)
        if (iniScripts != defaultScriptsDir) {
            scriptsDir = iniScripts
            prefs.edit().putString("scripts_dir", scriptsDir).apply()
        }
        val iniIcons = readIni(context, "settings", "icons_dir", defaultIconsDir)
        if (iniIcons != defaultIconsDir) {
            iconsDir = iniIcons
            prefs.edit().putString("icons_dir", iconsDir).apply()
        }
        iniFile = File(scriptsDir, "scripts.ini")
    }

    fun getScriptsDir(context: Context): String {
        return readIni(context, "settings", "scripts_dir", "/sdcard/MyScripts")
    }

    fun getIconsDir(context: Context): String {
        return readIni(context, "settings", "icons_dir", "/sdcard/MyScripts/icons")
    }

    fun updateSettings(context: Context, scriptsDir: String, iconsDir: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("scripts_dir", scriptsDir).putString("icons_dir", iconsDir).apply()
        writeIni(context, "settings", "scripts_dir", scriptsDir)
        writeIni(context, "settings", "icons_dir", iconsDir)
        iniFile = File(scriptsDir, "scripts.ini")
    }

    fun getScriptConfig(scriptName: String): ScriptConfig {
        val section = ini[scriptName]
        return if (section != null) {
            ScriptConfig(
                name = readIni(context, scriptName, "name", ""),
                description = readIni(context, scriptName, "description", ""),
                icon = readIni(context, scriptName, "icon", ""),
                isActive = readIni(context, scriptName, "is_active", "true").toBoolean(),
                hasShortcut = readIni(context, scriptName, "has_shortcut", "false").toBoolean()
            )
        } else {
            ScriptConfig(isActive = true)
        }
    }

    fun updateScriptConfig(scriptName: String, config: ScriptConfig) {
        writeIni(context, scriptName, "name", config.name)
        writeIni(context, scriptName, "description", config.description)
        writeIni(context, scriptName, "icon", config.icon)
        writeIni(context, scriptName, "is_active", config.isActive.toString())
        writeIni(context, scriptName, "has_shortcut", config.hasShortcut.toString())
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
        save(context)
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
            save(context)
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
}